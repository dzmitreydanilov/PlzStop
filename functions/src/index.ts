import { onCall, HttpsError } from "firebase-functions/v2/https";
import { VertexAI, GenerativeModel } from "@google-cloud/vertexai";

const projectId = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || "";

let cachedModel: GenerativeModel | null = null;

function getModel(): GenerativeModel {
  if (cachedModel) return cachedModel;
  const vertexAI = new VertexAI({ project: projectId, location: "europe-west1" });
  cachedModel = vertexAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    generationConfig: {
      responseMimeType: "application/json",
      // @ts-expect-error -- thinkingConfig is supported by 2.5 models
      thinkingConfig: { thinkingBudget: 0 },
    },
    systemInstruction: SYSTEM_PROMPT,
  });
  return cachedModel;
}

const SYSTEM_PROMPT = `Extract data from receipt, invoice, or bill images. Accept any format and country. If the image is not a financial document, return status "unreadable".

Response JSON:
{
  "status": "success" | "partial" | "unreadable",
  "data": {
    "merchantName": "string | null",
    "totalAmount": "number | null (decimal, e.g. 42.99)",
    "currency": "string | null (ISO 4217)",
    "date": "string | null (YYYY-MM-DD)",
    "categoryId": "number | null"
  },
  "message": "string | null"
}

Rules:
- totalAmount: use the FINAL total (including tax/tips), not the subtotal.
- currency: infer from the receipt's country or symbols if not printed.
- categoryId: the user provides a list of expense categories with IDs. Analyze the merchant name and purchased items on the receipt to pick the best matching category ID. Only use IDs from the provided list. Return null if no category fits.
- status: "success" if merchantName AND totalAmount extracted, "partial" if valid receipt but either is missing, "unreadable" if not a financial document.
- Return null for any uncertain or unreadable field.`;

interface Category {
  id: number;
  name: string;
}

interface ReceiptResponse {
  status: "success" | "partial" | "unreadable";
  data: {
    merchantName: string | null;
    totalAmount: number | null;
    currency: string | null;
    date: string | null;
    categoryId: number | null;
  } | null;
  message: string | null;
}

export const analyzeReceipt = onCall(
  {
    enforceAppCheck: false, // TODO: enable for production
    region: "europe-west1",
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request) => {
    const { imageBase64, categories } = request.data as {
      imageBase64?: string;
      categories?: Category[];
    };

    if (!imageBase64 || typeof imageBase64 !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_REQUEST: Missing imageBase64");
    }

    if (!categories || !Array.isArray(categories) || categories.length === 0) {
      throw new HttpsError("invalid-argument", "INVALID_REQUEST: Missing or empty categories");
    }

    // Check base64 decoded size (~5MB limit)
    const estimatedBytes = (imageBase64.length * 3) / 4;
    if (estimatedBytes > 5 * 1024 * 1024) {
      throw new HttpsError("invalid-argument", "IMAGE_TOO_LARGE: Image exceeds 5MB limit");
    }

    const categoriesText = categories
      .map((c) => `- ID: ${c.id}, Name: "${c.name}"`)
      .join("\n");

    const userPrompt = `Categories:\n${categoriesText}`;

    try {
      const model = getModel();

      const result = await model.generateContent({
        contents: [
          {
            role: "user",
            parts: [
              {
                inlineData: {
                  mimeType: "image/jpeg",
                  data: imageBase64,
                },
              },
              { text: userPrompt },
            ],
          },
        ],
      });

      const responseText =
        result.response?.candidates?.[0]?.content?.parts?.[0]?.text;

      if (!responseText) {
        return {
          status: "unreadable",
          data: null,
          message: "Could not process the image.",
        } satisfies ReceiptResponse;
      }

      let parsed: ReceiptResponse;
      try {
        parsed = JSON.parse(responseText);
      } catch {
        // Fallback: strip markdown code fences and retry
        const cleaned = responseText
          .replace(/^```json\s*/i, "")
          .replace(/```\s*$/i, "")
          .trim();
        try {
          parsed = JSON.parse(cleaned);
        } catch {
          return {
            status: "unreadable",
            data: null,
            message: "Could not parse the receipt data.",
          } satisfies ReceiptResponse;
        }
      }

      // Validate and normalize the response
      if (parsed.status === "unreadable") {
        return {
          status: "unreadable",
          data: null,
          message: parsed.message || "Couldn't read this receipt.",
        } satisfies ReceiptResponse;
      }

      return {
        status: parsed.status === "partial" ? "partial" : "success",
        data: {
          merchantName: parsed.data?.merchantName ?? null,
          totalAmount:
            typeof parsed.data?.totalAmount === "number"
              ? parsed.data.totalAmount
              : null,
          currency: parsed.data?.currency ?? null,
          date: parsed.data?.date ?? null,
          categoryId:
            typeof parsed.data?.categoryId === "number"
              ? parsed.data.categoryId
              : null,
        },
        message: parsed.message ?? null,
      } satisfies ReceiptResponse;
    } catch (error) {
      if (error instanceof HttpsError) {
        throw error;
      }

      const message =
        error instanceof Error ? error.message : "Unknown error";

      if (message.includes("quota") || message.includes("429")) {
        throw new HttpsError("resource-exhausted", "QUOTA_EXCEEDED: " + message);
      }

      throw new HttpsError("internal", "INTERNAL: " + message);
    }
  }
);

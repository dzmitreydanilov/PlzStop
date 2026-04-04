import { onCall, HttpsError } from "firebase-functions/v2/https";
import { VertexAI, GenerativeModel } from "@google-cloud/vertexai";
import { checkRateLimit } from "./rateLimit";
import { validateAndSanitize, Subcategory } from "./validation";

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

const SYSTEM_PROMPT = `Extract data from receipt, invoice, or bill images. Accept any format and country.

Response JSON:
{
  "status": "success" | "partial" | "not_receipt" | "unreadable",
  "data": {
    "merchantName": "string | null",
    "totalAmount": "number | null (decimal, e.g. 42.99)",
    "currency": "string | null (ISO 4217)",
    "date": "string | null (YYYY-MM-DD)",
    "categoryId": "number | null",
    "subcategoryId": "number | null"
  },
  "message": "string | null"
}

Rules:
- totalAmount: use the FINAL total (including tax/tips), not the subtotal.
- currency: infer from the receipt's country or symbols if not printed.
- categoryId: the user provides a list of expense categories with IDs. Analyze the merchant name and purchased items on the receipt to pick the best matching category ID. Only use IDs from the provided list. Return null if no category fits.
- subcategoryId: if subcategories are provided under a category, pick the best matching subcategory ID within the chosen category. Only use subcategory IDs listed under that specific category. Return null if no subcategory fits or if no subcategories are provided.
- status:
  - "success": merchantName AND totalAmount extracted.
  - "partial": valid receipt/invoice/bill but merchantName or totalAmount is missing.
  - "not_receipt": the image is NOT a financial document (e.g. selfie, screenshot, random photo).
  - "unreadable": the image looks like a receipt but is too blurry, cropped, or damaged to extract data.
- Return null for any uncertain or unreadable field.`;

type ReceiptStatus = "success" | "partial" | "not_receipt" | "unreadable" | "error";

const VALID_STATUSES: ReadonlySet<string> = new Set<ReceiptStatus>([
  "success",
  "partial",
  "not_receipt",
  "unreadable",
  "error",
]);

interface ReceiptResponse {
  status: ReceiptStatus;
  data: {
    merchantName: string | null;
    totalAmount: number | null;
    currency: string | null;
    date: string | null;
    categoryId: number | null;
    subcategoryId: number | null;
  } | null;
  message: string | null;
}

export const analyzeReceipt = onCall(
  {
    enforceAppCheck: false, // TODO: enable for production
    region: "europe-west1",
    timeoutSeconds: 120,
    memory: "512MiB",
    maxInstances: 10,
  },
  async (request) => {
    const clientIp = request.rawRequest.ip ?? "unknown";
    checkRateLimit(clientIp);

    const { imageBase64, categories, subcategories } =
      validateAndSanitize(request.data);

    const subcategoryMap = new Map<number, Subcategory[]>();
    for (const sub of subcategories) {
      const list = subcategoryMap.get(sub.parentCategoryId) ?? [];
      list.push(sub);
      subcategoryMap.set(sub.parentCategoryId, list);
    }

    const categoriesText = categories
      .map((c) => {
        const subs = subcategoryMap.get(c.id);
        const line = `- ID: ${c.id}, Name: "${c.name}"`;
        if (subs && subs.length > 0) {
          const subList = subs
            .map((s) => `[ID: ${s.id} "${s.name}"]`)
            .join(", ");
          return `${line}\n  Subcategories: ${subList}`;
        }
        return line;
      })
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
          status: "error",
          data: null,
          message: "Model returned an empty response.",
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
            status: "error",
            data: null,
            message: "Could not parse model response.",
          } satisfies ReceiptResponse;
        }
      }

      // Normalize status from model — it may return unexpected values
      const normalizedStatus: ReceiptStatus =
        typeof parsed.status === "string" && VALID_STATUSES.has(parsed.status)
          ? (parsed.status as ReceiptStatus)
          : "error";

      if (normalizedStatus === "not_receipt") {
        return {
          status: "not_receipt",
          data: null,
          message: parsed.message ?? null,
        } satisfies ReceiptResponse;
      }

      if (normalizedStatus === "unreadable") {
        return {
          status: "unreadable",
          data: null,
          message: parsed.message ?? null,
        } satisfies ReceiptResponse;
      }

      if (normalizedStatus === "error") {
        return {
          status: "error",
          data: null,
          message: parsed.message ?? "Unexpected model response.",
        } satisfies ReceiptResponse;
      }

      return {
        status: normalizedStatus === "partial" ? "partial" : "success",
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
          subcategoryId:
            typeof parsed.data?.subcategoryId === "number"
              ? parsed.data.subcategoryId
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

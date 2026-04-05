import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { defineSecret } from "firebase-functions/params";
import { VertexAI, GenerativeModel as VertexModel } from "@google-cloud/vertexai";
import {
  GoogleGenerativeAI,
  GenerativeModel as GeminiModel,
} from "@google/generative-ai";
import { cleanupExpiredRateLimits, checkGlobalDailyLimit } from "./rateLimit";
import {
  SYSTEM_PROMPT,
  ReceiptResponse,
  prepareRequest,
  parseResponse,
  handleError,
} from "./receipt";

// --- Vertex AI (GCP billing) ---

const projectId = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || "";

let cachedVertexModel: VertexModel | null = null;

function getVertexModel(): VertexModel {
  if (cachedVertexModel) return cachedVertexModel;
  const vertexAI = new VertexAI({ project: projectId, location: "europe-west1" });
  cachedVertexModel = vertexAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    generationConfig: {
      responseMimeType: "application/json",
      // @ts-expect-error -- thinkingConfig is supported by 2.5 models
      thinkingConfig: { thinkingBudget: 0 },
    },
    systemInstruction: SYSTEM_PROMPT,
  });
  return cachedVertexModel;
}

export const analyzeReceipt = onCall(
  {
    enforceAppCheck: false, // TODO: enable for production
    region: "europe-west1",
    timeoutSeconds: 120,
    memory: "512MiB",
    maxInstances: 10,
  },
  async (request): Promise<ReceiptResponse> => {
    try {
      const { imageBase64, userPrompt } = await prepareRequest(request);
      const model = getVertexModel();

      const result = await model.generateContent({
        contents: [
          {
            role: "user",
            parts: [
              { inlineData: { mimeType: "image/jpeg", data: imageBase64 } },
              { text: userPrompt },
            ],
          },
        ],
      });

      const responseText =
        result.response?.candidates?.[0]?.content?.parts?.[0]?.text;
      return parseResponse(responseText);
    } catch (error) {
      handleError(error);
    }
  }
);

// --- Gemini API (free tier, API key) ---

const geminiApiKey = defineSecret("GEMINI_API_KEY");

let cachedGeminiModel: GeminiModel | null = null;

function getGeminiModel(apiKey: string): GeminiModel {
  if (cachedGeminiModel) return cachedGeminiModel;
  const genAI = new GoogleGenerativeAI(apiKey);
  cachedGeminiModel = genAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    generationConfig: {
      responseMimeType: "application/json",
    },
    systemInstruction: SYSTEM_PROMPT,
  });
  return cachedGeminiModel;
}

export const analyzeReceiptGemini = onCall(
  {
    enforceAppCheck: false, // TODO: enable for production
    region: "europe-west1",
    timeoutSeconds: 120,
    memory: "512MiB",
    maxInstances: 10,
    secrets: [geminiApiKey],
  },
  async (request): Promise<ReceiptResponse> => {
    const apiKey = geminiApiKey.value();
    if (!apiKey) {
      throw new HttpsError(
        "failed-precondition",
        "Gemini API key is not configured."
      );
    }

    try {
      await checkGlobalDailyLimit();
      const { imageBase64, userPrompt } = await prepareRequest(request);
      const model = getGeminiModel(apiKey);

      const result = await model.generateContent([
        {
          inlineData: { mimeType: "image/jpeg", data: imageBase64 },
        },
        { text: userPrompt },
      ]);

      const responseText = result.response.text();
      return parseResponse(responseText);
    } catch (error) {
      handleError(error);
    }
  }
);

// --- Scheduled cleanup ---

export const rateLimitCleanup = onSchedule(
  { schedule: "every 24 hours", region: "europe-west1" },
  async () => {
    const deleted = await cleanupExpiredRateLimits();
    console.log(`rateLimitCleanup: deleted ${deleted} expired docs`);
  }
);

import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
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
  validateResponseData,
  handleError,
} from "./receipt";
export { cleanupGoogleOAuthOnUserDelete } from "./userCleanup";

// --- Vertex AI (GCP billing) ---

const projectId = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || "";

let cachedVertexModel: VertexModel | null = null;

function getVertexModel(): VertexModel {
  if (cachedVertexModel) return cachedVertexModel;
  const vertexAI = new VertexAI({project: projectId, location: "europe-west1"});
  cachedVertexModel = vertexAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    generationConfig: {
      responseMimeType: "application/json",
      // @ts-expect-error -- thinkingConfig is supported by 2.5 models
      thinkingConfig: {thinkingBudget: 0},
    },
    systemInstruction: SYSTEM_PROMPT,
  });
  return cachedVertexModel;
}

export const analyzeReceipt = onCall(
  {
    enforceAppCheck: true,
    region: "europe-west1",
    timeoutSeconds: 120,
    memory: "512MiB",
    maxInstances: 10,
  },
  async (request): Promise<ReceiptResponse> => {
    const startedAt = Date.now();
    try {
      const {imageBase64, userPrompt, validCategoryIds, subcategoryParents} =
        await prepareRequest(request);
      const model = getVertexModel();

      const result = await model.generateContent({
        contents: [
          {
            role: "user",
            parts: [
              {inlineData: {mimeType: "image/jpeg", data: imageBase64}},
              {text: userPrompt},
            ],
          },
        ],
      });

      const responseText =
        result.response?.candidates?.[0]?.content?.parts?.[0]?.text;
      logReceiptUsage("vertex", startedAt, result.response?.usageMetadata);
      const parsed = parseResponse(responseText);
      return validateResponseData(parsed, validCategoryIds, subcategoryParents);
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
    enforceAppCheck: true,
    region: "europe-west1",
    timeoutSeconds: 120,
    memory: "512MiB",
    maxInstances: 10,
    secrets: [geminiApiKey],
  },
  async (request): Promise<ReceiptResponse> => {
    const startedAt = Date.now();
    const apiKey = geminiApiKey.value();
    if (!apiKey) {
      throw new HttpsError(
        "failed-precondition",
        "Gemini API key is not configured."
      );
    }

    try {
      const { imageBase64, userPrompt, validCategoryIds, subcategoryParents } =
        await prepareRequest(request);
      await checkGlobalDailyLimit();
      const model = getGeminiModel(apiKey);

      const result = await model.generateContent([
        {
          inlineData: { mimeType: "image/jpeg", data: imageBase64 },
        },
        { text: userPrompt },
      ]);

      logReceiptUsage("developer_api", startedAt, result.response.usageMetadata);
      const responseText = result.response.text();
      const parsed = parseResponse(responseText);
      return validateResponseData(parsed, validCategoryIds, subcategoryParents);
    } catch (error) {
      handleError(error);
    }
  }
);

function logReceiptUsage(
  provider: string,
  startedAt: number,
  usage: unknown
): void {
  const usageRecord =
    typeof usage === "object" && usage !== null
      ? (usage as Record<string, unknown>)
      : {};
  logger.info("receipt_analysis_completed", {
    provider,
    durationMs: Date.now() - startedAt,
    promptTokenCount: usageRecord.promptTokenCount,
    candidatesTokenCount: usageRecord.candidatesTokenCount,
    totalTokenCount: usageRecord.totalTokenCount,
  });
}

// --- Scheduled cleanup ---

export const rateLimitCleanup = onSchedule(
  { schedule: "every 24 hours", region: "europe-west1" },
  async () => {
    const deleted = await cleanupExpiredRateLimits();
    console.log(`rateLimitCleanup: deleted ${deleted} expired docs`);
  }
);

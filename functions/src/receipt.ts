import { HttpsError } from "firebase-functions/v2/https";
import { checkRateLimit } from "./rateLimit";
import { validateAndSanitize, Subcategory } from "./validation";

export const SYSTEM_PROMPT = `Extract data from receipt, invoice, or bill images. Accept any format and country.

Response JSON:
{
  "status": "success" | "partial" | "not_receipt" | "unreadable",
  "data": {
    "merchantName": "string | null",
    "totalAmount": "number | null (decimal, e.g. 42.99)",
    "currency": "string | null (ISO 4217)",
    "date": "string | null (YYYY-MM-DD)",
    "items": [
      {
        "name": "string (short grouped label, e.g. 'Milk')",
        "amount": "number (combined total for the group)",
        "categoryId": "number | null",
        "subcategoryId": "number | null"
      }
    ]
  },
  "message": "string | null"
}

Rules:
- totalAmount: use the FINAL total (including tax/tips), not the subtotal.
- currency: infer from the receipt's country or symbols if not printed.
- items: group similar line items into a single entry. For example, "Oat milk" + "Lactose free milk" → one item named "Milk" with their prices summed. "Bread" + "Gluten free bread" → one item named "Bread". Use short, generic names for grouped items. If items are not similar, keep them separate. Each item must have a name and amount.
- categoryId / subcategoryId: assign per item. The user provides a list of expense categories with IDs. Analyze each item to pick the best matching category ID. Only use IDs from the provided list. If subcategories are provided under a category, pick the best matching subcategory ID within the chosen category. Return null if no category/subcategory fits.
- status:
  - "success": merchantName AND totalAmount extracted.
  - "partial": valid receipt/invoice/bill but merchantName or totalAmount is missing.
  - "not_receipt": the image is NOT a financial document (e.g. selfie, screenshot, random photo).
  - "unreadable": the image looks like a receipt but is too blurry, cropped, or damaged to extract data.
- Return null for any uncertain or unreadable field.
- items array can be empty if no individual items are readable.`;

export type ReceiptStatus =
  | "success"
  | "partial"
  | "not_receipt"
  | "unreadable"
  | "error";

export const VALID_STATUSES: ReadonlySet<string> = new Set<ReceiptStatus>([
  "success",
  "partial",
  "not_receipt",
  "unreadable",
  "error",
]);

export interface ReceiptItem {
  name: string;
  amount: number;
  categoryId: number | null;
  subcategoryId: number | null;
}

export interface ReceiptResponse {
  status: ReceiptStatus;
  data: {
    merchantName: string | null;
    totalAmount: number | null;
    currency: string | null;
    date: string | null;
    items: ReceiptItem[];
  } | null;
  message: string | null;
}

export interface PreparedRequest {
  imageBase64: string;
  userPrompt: string;
}

export async function prepareRequest(
  request: { rawRequest: { ip?: string }; data: Record<string, unknown> }
): Promise<PreparedRequest> {
  const clientIp = request.rawRequest.ip ?? "unknown";
  await checkRateLimit(clientIp);

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

  return { imageBase64, userPrompt: `Categories:\n${categoriesText}` };
}

export function parseResponse(responseText: string | undefined): ReceiptResponse {
  if (!responseText) {
    return { status: "error", data: null, message: "Model returned an empty response." };
  }

  let parsed: ReceiptResponse;
  try {
    parsed = JSON.parse(responseText);
  } catch {
    const cleaned = responseText
      .replace(/^```json\s*/i, "")
      .replace(/```\s*$/i, "")
      .trim();
    try {
      parsed = JSON.parse(cleaned);
    } catch {
      return { status: "error", data: null, message: "Could not parse model response." };
    }
  }

  const normalizedStatus: ReceiptStatus =
    typeof parsed.status === "string" && VALID_STATUSES.has(parsed.status)
      ? (parsed.status as ReceiptStatus)
      : "error";

  if (
    normalizedStatus === "not_receipt" ||
    normalizedStatus === "unreadable"
  ) {
    return { status: normalizedStatus, data: null, message: parsed.message ?? null };
  }

  if (normalizedStatus === "error") {
    return {
      status: "error",
      data: null,
      message: parsed.message ?? "Unexpected model response.",
    };
  }

  const rawItems: unknown[] = Array.isArray(parsed.data?.items)
    ? parsed.data.items
    : [];
  const items: ReceiptItem[] = (rawItems as Record<string, unknown>[])
    .filter(
      (item) =>
        typeof item.name === "string" && typeof item.amount === "number"
    )
    .map((item) => ({
      name: item.name as string,
      amount: item.amount as number,
      categoryId:
        typeof item.categoryId === "number" ? item.categoryId : null,
      subcategoryId:
        typeof item.subcategoryId === "number" ? item.subcategoryId : null,
    }));

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
      items,
    },
    message: parsed.message ?? null,
  };
}

export function handleError(error: unknown): never {
  if (error instanceof HttpsError) {
    throw error;
  }

  const errorMessage =
    error instanceof Error ? error.message : "Unknown error";
  console.error("analyzeReceipt error:", errorMessage);

  if (errorMessage.includes("quota") || errorMessage.includes("429")) {
    throw new HttpsError(
      "resource-exhausted",
      "Service temporarily unavailable. Please try again later."
    );
  }

  throw new HttpsError(
    "internal",
    "An unexpected error occurred. Please try again."
  );
}

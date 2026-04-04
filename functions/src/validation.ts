import { HttpsError } from "firebase-functions/v2/https";

const MAX_NAME_LENGTH = 100;
const MAX_CATEGORIES = 50;
const MAX_SUBCATEGORIES = 200;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

export interface Category {
  id: number;
  name: string;
}

export interface Subcategory {
  id: number;
  parentCategoryId: number;
  name: string;
}

export interface ValidatedInput {
  imageBase64: string;
  categories: Category[];
  subcategories: Subcategory[];
}

export function sanitizeName(name: unknown): string {
  if (typeof name !== "string") return "";
  return name
    .replace(/[\x00-\x1F\x7F]/g, "") // strip control characters
    .trim()
    .slice(0, MAX_NAME_LENGTH);
}

export function validateAndSanitize(data: Record<string, unknown>): ValidatedInput {
  const { imageBase64, categories, subcategories } = data as {
    imageBase64?: string;
    categories?: Category[];
    subcategories?: Subcategory[];
  };

  if (!imageBase64 || typeof imageBase64 !== "string") {
    throw new HttpsError("invalid-argument", "INVALID_REQUEST: Missing imageBase64");
  }

  if (!categories || !Array.isArray(categories) || categories.length === 0) {
    throw new HttpsError("invalid-argument", "INVALID_REQUEST: Missing or empty categories");
  }

  if (categories.length > MAX_CATEGORIES) {
    throw new HttpsError("invalid-argument", "INVALID_REQUEST: Too many categories");
  }

  const estimatedBytes = (imageBase64.length * 3) / 4;
  if (estimatedBytes > MAX_IMAGE_BYTES) {
    throw new HttpsError("invalid-argument", "IMAGE_TOO_LARGE: Image exceeds 5MB limit");
  }

  const sanitizedCategories: Category[] = categories.map((c) => ({
    id: c.id,
    name: sanitizeName(c.name),
  }));

  const sanitizedSubcategories: Subcategory[] = [];
  if (subcategories && Array.isArray(subcategories)) {
    if (subcategories.length > MAX_SUBCATEGORIES) {
      throw new HttpsError("invalid-argument", "INVALID_REQUEST: Too many subcategories");
    }
    for (const sub of subcategories) {
      sanitizedSubcategories.push({
        id: sub.id,
        parentCategoryId: sub.parentCategoryId,
        name: sanitizeName(sub.name),
      });
    }
  }

  return {
    imageBase64,
    categories: sanitizedCategories,
    subcategories: sanitizedSubcategories,
  };
}

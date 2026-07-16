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

  if (imageBase64.length % 4 !== 0 || !/^[A-Za-z0-9+/]+={0,2}$/.test(imageBase64)) {
    throw new HttpsError("invalid-argument", "INVALID_REQUEST: Invalid base64 encoding");
  }

  const categoryIds = new Set<number>();
  const sanitizedCategories: Category[] = categories.map((category) => {
    if (
      !category ||
      typeof category !== "object" ||
      typeof category.id !== "number" ||
      !Number.isInteger(category.id)
    ) {
      throw new HttpsError("invalid-argument", "INVALID_REQUEST: Category ID must be an integer");
    }
    if (categoryIds.has(category.id)) {
      throw new HttpsError("invalid-argument", "INVALID_REQUEST: Duplicate category ID");
    }
    const name = sanitizeName(category.name);
    if (!name) {
      throw new HttpsError("invalid-argument", "INVALID_REQUEST: Category name is required");
    }
    categoryIds.add(category.id);
    return { id: category.id, name };
  });

  const sanitizedSubcategories: Subcategory[] = [];
  const subcategoryIds = new Set<number>();
  if (subcategories && Array.isArray(subcategories)) {
    if (subcategories.length > MAX_SUBCATEGORIES) {
      throw new HttpsError("invalid-argument", "INVALID_REQUEST: Too many subcategories");
    }
    for (const subcategory of subcategories) {
      if (
        !subcategory ||
        typeof subcategory !== "object" ||
        typeof subcategory.id !== "number" ||
        !Number.isInteger(subcategory.id)
      ) {
        throw new HttpsError("invalid-argument", "INVALID_REQUEST: Subcategory ID must be an integer");
      }
      if (subcategoryIds.has(subcategory.id)) {
        throw new HttpsError("invalid-argument", "INVALID_REQUEST: Duplicate subcategory ID");
      }
      if (
        typeof subcategory.parentCategoryId !== "number" ||
        !Number.isInteger(subcategory.parentCategoryId) ||
        !categoryIds.has(subcategory.parentCategoryId)
      ) {
        throw new HttpsError("invalid-argument", "INVALID_REQUEST: Subcategory parentCategoryId must be an integer");
      }
      const name = sanitizeName(subcategory.name);
      if (!name) {
        throw new HttpsError("invalid-argument", "INVALID_REQUEST: Subcategory name is required");
      }
      subcategoryIds.add(subcategory.id);
      sanitizedSubcategories.push({
        id: subcategory.id,
        parentCategoryId: subcategory.parentCategoryId,
        name,
      });
    }
  } else if (subcategories !== undefined && subcategories !== null) {
    throw new HttpsError("invalid-argument", "INVALID_REQUEST: Subcategories must be an array");
  }

  return {
    imageBase64,
    categories: sanitizedCategories,
    subcategories: sanitizedSubcategories,
  };
}

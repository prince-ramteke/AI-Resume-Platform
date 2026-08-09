import { describe, expect, it } from "vitest";
import {
  JD_RAW_TEXT_MAX,
  JD_TITLE_MAX,
  RESUME_MAX_BYTES,
  validateConfirm,
  validateEmail,
  validateJdFile,
  validateJdRawText,
  validateJdTitle,
  validateLoginPassword,
  validatePassword,
  validateResumeFile,
} from "../lib/validators";

/**
 * Client-side validators are UX only, but they mirror the backend Bean
 * Validation constraints — a divergence is a real bug. Each rule is tested for
 * one happy case and its most common failure(s); goal is regression coverage,
 * not exhaustive fuzzing.
 */

// jsdom's File constructor is sufficient for size/name checks; the backend
// re-validates magic bytes authoritatively so we don't need real file content.
function fakeFile(name: string, size: number, type = ""): File {
  const content = new Uint8Array(size);
  return new File([content], name, { type });
}

describe("validateEmail", () => {
  it("accepts a valid address", () => {
    expect(validateEmail("prince@example.com")).toBeUndefined();
  });
  it("rejects blank input", () => {
    expect(validateEmail("")).toMatch(/enter your email/i);
    expect(validateEmail("   ")).toMatch(/enter your email/i);
  });
  it("rejects malformed input", () => {
    expect(validateEmail("not-an-email")).toMatch(/valid email/i);
    expect(validateEmail("a@b")).toMatch(/valid email/i);
  });
});

describe("validateLoginPassword", () => {
  it("accepts any non-empty password (login doesn't re-check composition)", () => {
    expect(validateLoginPassword("x")).toBeUndefined();
  });
  it("rejects empty input", () => {
    expect(validateLoginPassword("")).toMatch(/enter your password/i);
  });
});

describe("validatePassword (register)", () => {
  it("accepts min-length + letter + digit", () => {
    expect(validatePassword("Password1")).toBeUndefined();
  });
  it("rejects too-short input", () => {
    expect(validatePassword("Ab1")).toMatch(/8 characters/);
  });
  it("rejects letters-only", () => {
    expect(validatePassword("abcdefgh")).toMatch(/number/);
  });
  it("rejects digits-only", () => {
    expect(validatePassword("12345678")).toMatch(/letter/);
  });
});

describe("validateConfirm", () => {
  it("accepts matching passwords", () => {
    expect(validateConfirm("Pass1word", "Pass1word")).toBeUndefined();
  });
  it("rejects blank confirmation", () => {
    expect(validateConfirm("Pass1word", "")).toMatch(/re-enter/i);
  });
  it("rejects mismatch", () => {
    expect(validateConfirm("Pass1word", "Different1")).toMatch(/don't match/i);
  });
});

describe("validateResumeFile", () => {
  it("accepts a PDF within size", () => {
    expect(validateResumeFile(fakeFile("cv.pdf", 1_000))).toBeUndefined();
  });
  it("accepts a DOCX within size", () => {
    expect(validateResumeFile(fakeFile("cv.docx", 1_000))).toBeUndefined();
  });
  it("rejects null", () => {
    expect(validateResumeFile(null)).toMatch(/PDF or DOCX/);
  });
  it("rejects wrong extension", () => {
    expect(validateResumeFile(fakeFile("cv.txt", 1_000))).toMatch(/PDF or DOCX/);
  });
  it("rejects empty", () => {
    expect(validateResumeFile(fakeFile("cv.pdf", 0))).toMatch(/empty/);
  });
  it("rejects >10MB", () => {
    expect(validateResumeFile(fakeFile("cv.pdf", RESUME_MAX_BYTES + 1))).toMatch(
      /10 MB/
    );
  });
});

describe("validateJdTitle", () => {
  it("accepts a normal title", () => {
    expect(validateJdTitle("Backend Engineer")).toBeUndefined();
  });
  it("rejects blank", () => {
    expect(validateJdTitle("   ")).toMatch(/enter a title/i);
  });
  it("rejects too-long", () => {
    expect(validateJdTitle("a".repeat(JD_TITLE_MAX + 1))).toMatch(/255/);
  });
});

describe("validateJdRawText", () => {
  it("accepts a normal paste", () => {
    expect(validateJdRawText("We are hiring")).toBeUndefined();
  });
  it("rejects blank", () => {
    expect(validateJdRawText("")).toMatch(/enter the job description/i);
  });
  it("rejects too-long", () => {
    expect(validateJdRawText("a".repeat(JD_RAW_TEXT_MAX + 1))).toMatch(/50/);
  });
});

describe("validateJdFile", () => {
  it("accepts PDF/DOCX/TXT within size", () => {
    expect(validateJdFile(fakeFile("jd.pdf", 500))).toBeUndefined();
    expect(validateJdFile(fakeFile("jd.docx", 500))).toBeUndefined();
    expect(validateJdFile(fakeFile("jd.txt", 500))).toBeUndefined();
  });
  it("rejects wrong extension", () => {
    expect(validateJdFile(fakeFile("jd.md", 500))).toMatch(/PDF, DOCX, or TXT/);
  });
  it("rejects empty", () => {
    expect(validateJdFile(fakeFile("jd.txt", 0))).toMatch(/empty/);
  });
});

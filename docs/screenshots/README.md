# Screenshot Capture Checklist

Screenshots for the README are captured from the live production app at
[ai-resume-platform.pages.dev](https://ai-resume-platform.pages.dev).

## Naming Convention

Files go in this directory (`docs/screenshots/`). Use the exact filenames below
so the README can reference them without edits.

## Capture List

| Filename | Screen | Notes |
|---|---|---|
| `01-landing.png` | Marketing landing page | Hero section visible; pipeline illustration showing |
| `02-register.png` | Registration form | Email + password fields; before OTP step |
| `03-verify-email.png` | Email verification | 6-digit OTP input; resend cooldown visible |
| `04-dashboard.png` | Authenticated dashboard | KPI strip (resume count, JD count, analyses count); new-analysis banner |
| `05-resume-upload.png` | Resume library | Grid of uploaded resumes; upload dropzone visible |
| `06-new-analysis.png` | New analysis setup | Resume + JD selector; pipeline track in idle state |
| `07-analysis-result.png` | Analysis results | Score dial; matched/missing/weak skill badges |
| `08-evidence-panel.png` | Evidence thread | JD requirements ↔ resume chunk connections |

## Capture Guidelines

- **Browser:** Chrome or Firefox; viewport 1440 × 900
- **Theme:** capture both light and dark if the toggle is visible; default to light
- **Format:** PNG, ≤ 400 KB per image (compress with `pngquant` or equivalent if larger)
- **Privacy:** do not use real personal data — use a sample resume and a fictional job description
- **Infrastructure screens excluded:** do not include Render, GitHub Actions,
  Brevo, Docker, or Grafana screenshots in the main README section

## After Capture

Update the README `## 3. Screenshots` section to replace the table rows
with actual `![alt](docs/screenshots/<filename>)` image tags.

# Self-hosted fonts

The app self-hosts three variable fonts (no external requests, no npm packages).
Drop the following `.woff2` files into this directory — the `@font-face` rules in
`src/index.css` reference them by exact name:

| File | Family | Source (OFL, free) |
|---|---|---|
| `Fraunces-Variable.woff2` | Fraunces (display) | https://fonts.google.com/specimen/Fraunces |
| `Inter-Variable.woff2` | Inter (body / UI) | https://fonts.google.com/specimen/Inter |
| `JetBrainsMono-Variable.woff2` | JetBrains Mono (data) | https://fonts.google.com/specimen/JetBrains+Mono |

All three are OFL-licensed. Easiest way to get the `.woff2` variable files:

- Download from Google Fonts (or https://gwfh.mranftl.com to get `woff2`), **or**
- Grab the variable `woff2` from the Fontsource CDN, e.g.
  `@fontsource-variable/inter`, `@fontsource-variable/fraunces`,
  `@fontsource-variable/jetbrains-mono` (copy the file out — do not add the npm dep).

Rename each to the filename in the table above.

**Until the files are added,** the UI renders with the system fallback stack declared
in `@theme` (`ui-serif` / `ui-sans-serif` / `ui-monospace`) — the app is fully usable
and on-brand; only the exact typefaces differ. `font-display: swap` prevents invisible
text.

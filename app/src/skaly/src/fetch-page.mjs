import { writeFile } from 'node:fs/promises'

const url = process.argv[2]
const output = process.argv[3] || 'tmp-page.html'

if (!url) {
  console.error('Usage: node src/fetch-page.mjs <url> [output]')
  process.exit(1)
}

const response = await fetch(url, {
  headers: {
    'user-agent': 'Mozilla/5.0 (compatible; skaly CHS inspector)',
    accept: 'text/html,application/xhtml+xml',
  },
  cache: 'no-store',
})

if (!response.ok) {
  throw new Error(`HTTP ${response.status} for ${url}`)
}

const html = await response.text()
await writeFile(output, html, 'utf8')
console.log(output)

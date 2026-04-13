const response = await fetch('https://www.horosvaz.cz/databaze-skal-cr/', {
  headers: {
    'user-agent': 'Mozilla/5.0 (compatible; skaly CHS inspector)',
    accept: 'text/html,application/xhtml+xml',
  },
  cache: 'no-store',
})

const html = await response.text()
const ids = new Set()
const regex = /skaly-region-(\d+)/gi

for (const match of html.matchAll(regex)) {
  ids.add(Number(match[1]))
}

console.log(JSON.stringify({ regions: ids.size, ids: [...ids].sort((a, b) => a - b) }, null, 2))

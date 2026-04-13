import { readFile } from 'node:fs/promises'

const path = process.argv[2] || 'data/chs-complete.json'
const needle = process.argv[3] || null
const raw = await readFile(path, 'utf8')
const data = JSON.parse(raw)

if (!needle) {
  console.log(
    JSON.stringify(
      {
        generatedAt: data.generatedAt,
        stats: data.stats,
        regions: data.regions.length,
        firstRegion: data.regions[0]?.name || null,
        lastRegion: data.regions[data.regions.length - 1]?.name || null,
      },
      null,
      2,
    ),
  )
  process.exit(0)
}

for (const region of data.regions) {
  for (const group of region.groups || []) {
    for (const area of group.areas || []) {
      if (area.name.includes(needle)) {
        console.log(
          JSON.stringify(
            {
              region: region.name,
              group: group.name,
              area: {
                id: area.id,
                name: area.name,
                gps: area.gps,
                sectors: area.sectors.slice(0, 10).map((sector) => sector.name),
              },
            },
            null,
            2,
          ),
        )
        process.exit(0)
      }

      for (const sector of area.sectors || []) {
        if (sector.name.includes(needle)) {
          console.log(
            JSON.stringify(
              {
                region: region.name,
                group: group.name,
                area: area.name,
                sector: {
                  id: sector.id,
                  name: sector.name,
                  gps: sector.gps,
                  routes: sector.routes.slice(0, 25).map((route) => route.name),
                },
              },
              null,
              2,
            ),
          )
          process.exit(0)
        }
      }
    }
  }
}

console.error(`Not found: ${needle}`)
process.exit(1)

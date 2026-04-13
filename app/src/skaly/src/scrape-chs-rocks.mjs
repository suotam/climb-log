import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'

const BASE_URL = 'https://www.horosvaz.cz'
const ROOT_URL = `${BASE_URL}/databaze-skal-cr/`

function decodeHtmlEntities(value) {
  return value
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)))
}

function stripTags(value) {
  return value.replace(/<[^>]+>/g, ' ')
}

function normalizeWhitespace(value) {
  return decodeHtmlEntities(stripTags(value)).replace(/\s+/g, ' ').trim()
}

function toAbsoluteUrl(url) {
  if (!url) {
    return null
  }

  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  return new URL(url, BASE_URL).toString()
}

function parseArgs(argv) {
  const options = {
    output: 'data/chs-complete.json',
    delayMs: 120,
    maxRegions: null,
    maxGroups: null,
    maxAreas: null,
    maxSectors: null,
    regionId: null,
    groupId: null,
    areaId: null,
    resume: true,
  }

  for (let index = 0; index < argv.length; index += 1) {
    const current = argv[index]
    const next = argv[index + 1]

    if (current === '--output' && next) {
      options.output = next
      index += 1
      continue
    }

    if (current === '--delay-ms' && next) {
      options.delayMs = Number(next)
      index += 1
      continue
    }

    if (current === '--max-regions' && next) {
      options.maxRegions = Number(next)
      index += 1
      continue
    }

    if (current === '--max-groups' && next) {
      options.maxGroups = Number(next)
      index += 1
      continue
    }

    if (current === '--max-areas' && next) {
      options.maxAreas = Number(next)
      index += 1
      continue
    }

    if (current === '--max-sectors' && next) {
      options.maxSectors = Number(next)
      index += 1
      continue
    }

    if (current === '--region-id' && next) {
      options.regionId = Number(next)
      index += 1
      continue
    }

    if (current === '--group-id' && next) {
      options.groupId = Number(next)
      index += 1
      continue
    }

    if (current === '--area-id' && next) {
      options.areaId = Number(next)
      index += 1
      continue
    }

    if (current === '--no-resume') {
      options.resume = false
    }
  }

  return options
}

async function sleep(ms) {
  await new Promise((resolvePromise) => setTimeout(resolvePromise, ms))
}

async function writeJsonFile(path, data) {
  await mkdir(dirname(path), { recursive: true })
  await writeFile(path, `${JSON.stringify(data, null, 2)}\n`, 'utf8')
}

async function readExistingJson(path) {
  try {
    const content = await readFile(path, 'utf8')
    return JSON.parse(content)
  } catch {
    return null
  }
}

async function fetchHtml(url) {
  const maxAttempts = 5

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const response = await fetch(url, {
        headers: {
          'user-agent': 'Mozilla/5.0 (compatible; skaly CHS scraper)',
          accept: 'text/html,application/xhtml+xml',
        },
        cache: 'no-store',
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status} for ${url}`)
      }

      return await response.text()
    } catch (error) {
      if (attempt === maxAttempts) {
        throw error
      }

      const backoffMs = attempt * 2000
      console.warn(`Retry ${attempt}/${maxAttempts - 1} after failure for ${url}: ${error.message}`)
      await sleep(backoffMs)
    }
  }

  throw new Error(`Failed to fetch ${url}`)
}

function limitItems(items, limit) {
  if (limit == null || !Number.isFinite(limit) || limit < 0) {
    return items
  }

  return items.slice(0, limit)
}

function extractBalancedTag(html, selectorToken, tagName) {
  const lowerHtml = html.toLowerCase()
  const lowerSelector = selectorToken.toLowerCase()
  const startIndex = lowerHtml.indexOf(lowerSelector)
  if (startIndex < 0) {
    return null
  }

  const openTagStart = lowerHtml.lastIndexOf(`<${tagName}`, startIndex)
  if (openTagStart < 0) {
    return null
  }

  let index = openTagStart
  let depth = 0

  while (index < html.length) {
    const nextOpen = lowerHtml.indexOf(`<${tagName}`, index)
    const nextClose = lowerHtml.indexOf(`</${tagName}>`, index)

    if (nextClose < 0) {
      break
    }

    if (nextOpen >= 0 && nextOpen < nextClose) {
      depth += 1
      index = nextOpen + tagName.length + 1
      continue
    }

    depth -= 1
    index = nextClose + tagName.length + 3

    if (depth === 0) {
      return html.slice(openTagStart, index)
    }
  }

  return null
}

function extractDirectChildBlocks(containerHtml, tagName) {
  const blocks = []
  const openRegex = new RegExp(`<${tagName}(\\s|>)`, 'ig')
  const closeRegex = new RegExp(`</${tagName}>`, 'ig')
  let depth = 0
  let blockStart = -1
  let cursor = 0

  while (cursor < containerHtml.length) {
    openRegex.lastIndex = cursor
    closeRegex.lastIndex = cursor

    const openMatch = openRegex.exec(containerHtml)
    const closeMatch = closeRegex.exec(containerHtml)

    const nextOpen = openMatch ? openMatch.index : -1
    const nextClose = closeMatch ? closeMatch.index : -1

    if (nextOpen === -1 && nextClose === -1) {
      break
    }

    if (nextOpen !== -1 && (nextClose === -1 || nextOpen < nextClose)) {
      if (depth === 0) {
        blockStart = nextOpen
      }
      depth += 1
      cursor = nextOpen + 1
      continue
    }

    depth -= 1
    cursor = nextClose + tagName.length + 3

    if (depth === 0 && blockStart >= 0) {
      blocks.push(containerHtml.slice(blockStart, cursor))
      blockStart = -1
    }
  }

  return blocks
}

function cleanMassifSuffix(name) {
  return name.replace(/\s+-\s+[A-ZÁČĎÉĚÍŇÓŘŠŤÚŮÝŽ]+$/u, '').trim()
}

function parseEntityIdFromUrl(url, prefix) {
  const match = url.match(new RegExp(`${prefix}(\\d+)`, 'i'))
  return match ? Number(match[1]) : null
}

function parseChildEntriesFromMainList(html, prefix) {
  const mainListHtml = extractBalancedTag(html, 'id="main-list"', 'ul')
  if (!mainListHtml) {
    return []
  }

  const liBlocks = extractDirectChildBlocks(mainListHtml, 'li')
  const entries = []

  for (const liBlock of liBlocks) {
    const infoTextMatch = liBlock.match(/<div class="info-text[\s\S]*?<\/div>/i)
    const anchorMatch = infoTextMatch?.[0]?.match(/<a\b[^>]*href="([^"]+)"[^>]*title="([^"]*)"[^>]*>([\s\S]*?)<\/a>/i)
    if (!anchorMatch) {
      continue
    }

    const href = anchorMatch[1]
    const id = parseEntityIdFromUrl(href, prefix)
    if (id == null) {
      continue
    }

    const iteratorMatch = infoTextMatch[0].match(/<span class="iterator">(\d+)\)<\/span>/i)
    const name = cleanMassifSuffix(normalizeWhitespace(anchorMatch[3] || anchorMatch[2] || ''))

    entries.push({
      id,
      name,
      url: toAbsoluteUrl(href),
      order: iteratorMatch ? Number(iteratorMatch[1]) : entries.length + 1,
      html: liBlock,
    })
  }

  return entries
}

function parseGpsFromMapyUrl(html) {
  const match = html.match(/https:\/\/mapy\.cz\/[^"' ]*?\?y=([0-9.]+)&x=([0-9.]+)/i)
  if (!match) {
    return null
  }

  return {
    lat: Number(match[1]),
    lng: Number(match[2]),
  }
}

function parseRockTypeFromPage(html) {
  const match = html.match(/<span[^>]+title="hornina a limit"[^>]*>([^<]+)<\/span>/i)
  return match ? normalizeWhitespace(match[1]) : null
}

function parseDescriptionFromPage(html) {
  const mountainsTextHtml = extractBalancedTag(html, 'class="mountains-text"', 'div')
  if (!mountainsTextHtml) return null
  const paragraphs = mountainsTextHtml.match(/<p[^>]*>([\s\S]*?)<\/p>/gi) || []
  const texts = paragraphs
    .map((p) => normalizeWhitespace(p))
    .filter((t) => t.length > 20) // skip very short/empty paragraphs
  return texts.join(' ') || null
}

function parseMapCodeUrl(html) {
  const match = html.match(/id="GoogleMapWrapper"[^>]*data-url="([^"]+)"/i)
  return toAbsoluteUrl(match?.[1] || null)
}

function parseGpsMarkersFromMapCode(html) {
  const markers = {
    area: null,
    sectorsById: new Map(),
  }

  const addRegex = /\.add\((\{[\s\S]*?\})\);/g

  for (const match of html.matchAll(addRegex)) {
    const rawJson = match[1]
      .replace(/\\\//g, '/')
      .replace(/\u0000/g, '')

    try {
      const marker = JSON.parse(rawJson)
      const eventUrl = marker?.events?.[0]?.url || ''
      const typeMatch = eventUrl.match(/type=([a-z]+)&id=(\d+)/i)
      if (!typeMatch) {
        continue
      }

      const gps = {
        lat: Number(marker.lat),
        lng: Number(marker.lng),
      }

      if (typeMatch[1] === 'sektor') {
        markers.area = gps
      }

      if (typeMatch[1] === 'skala') {
        markers.sectorsById.set(Number(typeMatch[2]), gps)
      }
    } catch {
      continue
    }
  }

  return markers
}

function parseRouteTypeFromLi(liBlock) {
  if (/icons-sedomodra-sportovni_ano17/.test(liBlock)) return 'sport'
  if (/icons-sedomodra-tradicni_lezeni_ano17/.test(liBlock)) return 'trad'
  if (/icons-sedomodra-trad-piskovcove_ano17/.test(liBlock)) return 'trad'
  if (/icons-sedomodra-bouldering_ano17/.test(liBlock)) return 'boulder'
  return null
}

function parseRouteFromLi(liBlock, fallbackOrder) {
  const infoTextMatch = liBlock.match(/<div class="info-text[\s\S]*?<\/div>/i)
  const anchorMatch = infoTextMatch?.[0]?.match(/<a\b[^>]*href="([^"]+)"[^>]*title="([^"]*)"[^>]*>([\s\S]*?)<\/a>/i)
  if (!anchorMatch) {
    return null
  }

  const href = anchorMatch[1]
  const id = parseEntityIdFromUrl(href, 'skaly-cesta-')
  if (id == null) {
    return null
  }

  const iteratorMatch = infoTextMatch[0].match(/<span class="iterator">(\d+)\)<\/span>/i)
  const name = normalizeWhitespace(anchorMatch[3] || anchorMatch[2] || '')

  // Use extractBalancedTag to correctly handle nested spans (e.g. stars rating span inside info-roads)
  const infoRoadsHtml = extractBalancedTag(liBlock, 'class="info-roads"', 'span') || ''
  const infoTokens = normalizeWhitespace(infoRoadsHtml)
    .split(',')
    .map((token) => token.trim())
    .filter(Boolean)

  let rating = null
  let grade = null
  let length = null

  for (const token of infoTokens) {
    if (/^\*+$/.test(token)) {
      rating = token.length
      continue
    }

    if (/^\d+\s*m$/i.test(token)) {
      length = token
      continue
    }

    if (grade == null) {
      grade = token
    }
  }

  const nestedUl = extractBalancedTag(liBlock, '<ul', 'ul')
  const detailLis = nestedUl ? extractDirectChildBlocks(nestedUl, 'li') : []
  const detailLines = detailLis.map((item) => normalizeWhitespace(item)).filter(Boolean)

  return {
    id,
    order: iteratorMatch ? Number(iteratorMatch[1]) : fallbackOrder,
    name,
    url: toAbsoluteUrl(href),
    grade,
    length,
    rating,
    type: parseRouteTypeFromLi(liBlock),
    description: detailLines[0] || null,
    firstAscent: detailLines[1] || null,
  }
}

function parseRoutesFromRockPage(html) {
  const mainListHtml = extractBalancedTag(html, 'id="main-list"', 'ul')
  if (!mainListHtml) {
    return []
  }

  const liBlocks = extractDirectChildBlocks(mainListHtml, 'li')
  return liBlocks
    .map((liBlock, index) => parseRouteFromLi(liBlock, index + 1))
    .filter(Boolean)
}

function countStats(regions) {
  let groups = 0
  let areas = 0
  let sectors = 0
  let routes = 0

  for (const region of regions) {
    groups += region.groups.length
    for (const group of region.groups) {
      areas += group.areas.length
      for (const area of group.areas) {
        sectors += area.sectors.length
        for (const sector of area.sectors) {
          routes += sector.routes.length
        }
      }
    }
  }

  return {
    regions: regions.length,
    groups,
    areas,
    sectors,
    routes,
  }
}

async function fetchRegionLinks(delayMs) {
  const html = await fetchHtml(ROOT_URL)
  if (delayMs > 0) {
    await sleep(delayMs)
  }

  const navHtml = extractBalancedTag(html, 'class="infinite-list-menu"', 'div') || html
  const links = []
  const anchorRegex = /<a\b[^>]*href="([^"]*skaly-region-\d+[^"]*)"[^>]*title="([^"]*)"[^>]*>/gi
  const seen = new Set()

  for (const match of navHtml.matchAll(anchorRegex)) {
    const href = match[1]
    const id = parseEntityIdFromUrl(href, 'skaly-region-')
    if (id == null || seen.has(id)) {
      continue
    }

    seen.add(id)
    links.push({
      id,
      name: normalizeWhitespace(match[2]),
      url: toAbsoluteUrl(href),
    })
  }

  return links
}

async function scrapeSector(sectorLink, areaMarkerGps, delayMs) {
  const html = await fetchHtml(sectorLink.url)
  const routes = parseRoutesFromRockPage(html)
  const gps = areaMarkerGps || parseGpsFromMapyUrl(html)

  if (delayMs > 0) {
    await sleep(delayMs)
  }

  return {
    id: sectorLink.id,
    order: sectorLink.order,
    name: sectorLink.name,
    url: sectorLink.url,
    official_type: 'skala',
    gps,
    description: parseDescriptionFromPage(html),
    routes,
  }
}

async function scrapeArea(areaLink, options) {
  console.log(`Area: ${areaLink.name}`)
  const html = await fetchHtml(areaLink.url)
  const sectorLinks = limitItems(parseChildEntriesFromMainList(html, 'skaly-skala-'), options.maxSectors)
  const mapCodeUrl = parseMapCodeUrl(html)
  let areaGps = null
  let sectorGpsById = new Map()

  if (mapCodeUrl) {
    try {
      const mapCodeHtml = await fetchHtml(mapCodeUrl)
      const markers = parseGpsMarkersFromMapCode(mapCodeHtml)
      areaGps = markers.area
      sectorGpsById = markers.sectorsById
      if (options.delayMs > 0) {
        await sleep(options.delayMs)
      }
    } catch (error) {
      console.warn(`Map GPS unavailable for ${areaLink.url}: ${error.message}`)
    }
  }

  if (options.delayMs > 0) {
    await sleep(options.delayMs)
  }

  const sectors = []

  for (const sectorLink of sectorLinks) {
    sectors.push(await scrapeSector(sectorLink, sectorGpsById.get(sectorLink.id) || null, options.delayMs))
  }

  return {
    id: areaLink.id,
    name: areaLink.name,
    url: areaLink.url,
    official_type: 'sektor',
    gps: areaGps,
    rockType: parseRockTypeFromPage(html),
    description: parseDescriptionFromPage(html),
    sectors,
  }
}

async function scrapeGroup(groupLink, options) {
  console.log(`Group: ${groupLink.name}`)
  const html = await fetchHtml(groupLink.url)
  const areaLinks = limitItems(parseChildEntriesFromMainList(html, 'skaly-sektor-'), options.maxAreas)
    .filter((areaLink) => options.areaId == null || areaLink.id === options.areaId)

  if (options.delayMs > 0) {
    await sleep(options.delayMs)
  }

  const areas = []

  for (const areaLink of areaLinks) {
    areas.push(await scrapeArea(areaLink, options))
  }

  return {
    id: groupLink.id,
    name: groupLink.name,
    url: groupLink.url,
    official_type: 'oblast',
    areas,
  }
}

async function scrapeRegion(regionLink, options) {
  console.log(`Region: ${regionLink.name}`)
  const html = await fetchHtml(regionLink.url)
  const groupLinks = limitItems(parseChildEntriesFromMainList(html, 'skaly-oblast-'), options.maxGroups)
    .filter((groupLink) => options.groupId == null || groupLink.id === options.groupId)

  if (options.delayMs > 0) {
    await sleep(options.delayMs)
  }

  const groups = []

  for (const groupLink of groupLinks) {
    groups.push(await scrapeGroup(groupLink, options))
  }

  return {
    id: regionLink.id,
    name: regionLink.name,
    url: regionLink.url,
    groups,
  }
}

function shouldResume(existing) {
  return existing && Array.isArray(existing.regions) && existing.regions.every((region) => Array.isArray(region.groups))
}

const TEST_REGION_NAMES = new Set(['ztest', 'test'])

async function scrapeDatabase(options, outputPath) {
  const existing = options.resume ? await readExistingJson(outputPath) : null
  const completedRegions = shouldResume(existing)
    ? new Map(existing.regions.map((region) => [region.id, region]))
    : new Map()
  const regionLinks = limitItems(await fetchRegionLinks(options.delayMs), options.maxRegions)
    .filter((regionLink) => options.regionId == null || regionLink.id === options.regionId)
    .filter((regionLink) => !TEST_REGION_NAMES.has(regionLink.name.toLowerCase().trim()))
  const regions = []

  for (const regionLink of regionLinks) {
    if (completedRegions.has(regionLink.id)) {
      regions.push(completedRegions.get(regionLink.id))
      console.log(`Region: ${regionLink.name} (resume)`)
      continue
    }

    regions.push(await scrapeRegion(regionLink, options))

    await writeJsonFile(outputPath, {
      generatedAt: new Date().toISOString(),
      source: ROOT_URL,
      stats: countStats(regions),
      regions,
    })
  }

  return {
    generatedAt: new Date().toISOString(),
    source: ROOT_URL,
    schema: {
      region: 'skaly-region-*',
      group: 'skaly-oblast-*',
      area: 'skaly-sektor-*',
      sector: 'skaly-skala-*',
      route: 'skaly-cesta-*',
    },
    stats: countStats(regions),
    regions,
  }
}

async function main() {
  const options = parseArgs(process.argv.slice(2))
  const outputPath = resolve(process.cwd(), options.output)
  const result = await scrapeDatabase(options, outputPath)
  await writeJsonFile(outputPath, result)
  console.log(`Saved ${result.stats.routes} routes to ${outputPath}`)
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})

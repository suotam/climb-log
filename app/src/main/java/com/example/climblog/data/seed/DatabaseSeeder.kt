package com.example.climblog.data.seed

import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.dao.SectorDao
import com.example.climblog.data.local.entity.AreaEntity
import com.example.climblog.data.local.entity.RouteEntity
import com.example.climblog.data.local.entity.SectorEntity
import com.example.climblog.domain.model.GradeSystem
import com.example.climblog.domain.model.RouteType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val areaDao: AreaDao,
    private val sectorDao: SectorDao,
    private val routeDao: RouteDao
) {
    suspend fun seedIfEmpty() {
        if (areaDao.count() > 0) return
        seedMoravskyKras()
        seedLabskePiskovce()
        seedJestedy()
    }

    suspend fun seedRezIfMissing() {
        if (areaDao.countByName("Řež") > 0) return
        seedRez()
    }

    // ── Moravský kras ─────────────────────────────────────────────────────────

    private suspend fun seedMoravskyKras() {
        val areaId = areaDao.insert(
            AreaEntity(
                name = "Moravský kras",
                country = "CZ",
                region = "Jihomoravský kraj",
                description = "Největší krasová oblast střední Evropy s výbornými sportovními cestami na vápenci.",
                rockType = "Vápenec",
                latitude = 49.376,
                longitude = 16.714
            )
        )

        val sloup = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "Sloupský kotel",
                description = "Kompaktní sektor s krátkými sportovními cestami. Orientace S, suché i v dešti.",
                approach = "Z parkiště ve Sloupu 10 min po červené značce."
            )
        )

        routeDao.insertAll(listOf(
            route(sloup, "Sůlová věž", "5c", 12, 8, "Klasická vstupní cesta s dobrými chyty.", RouteType.SPORT),
            route(sloup, "Letní přímka", "6a", 15, 10, "Příjemná přímá linie středem stěny.", RouteType.SPORT),
            route(sloup, "Za vánkem", "6b", 18, 11, "Technická cesta s klíčovým krokem v horní třetině.", RouteType.SPORT),
            route(sloup, "Tanec se stěnou", "6c", 20, 12, "Dynamická cesta na přelitech, vyžaduje dobrý footwork.", RouteType.SPORT),
            route(sloup, "Přímá cesta", "7a", 22, 13, "Klasická obtížnostní cesta oblasti. Zkoušení od dola.", RouteType.SPORT),
            route(sloup, "Siréna", "7b+", 24, 14, "Projekt místních. Tuhý start, pak krátká úleva a závěrečný přelit.", RouteType.SPORT)
        ))

        val ostrov = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "Ostrov u Macochy",
                description = "Mohutná převislá stěna nad Ostrovem. Delší cesty s dobrým jištěním.",
                approach = "Z Ostrova u Macochy 15 min lesní cestou."
            )
        )

        routeDao.insertAll(listOf(
            route(ostrov, "Koupel v oblacích", "6a+", 25, 14, "Dlouhá cesta stěnou, krásný výhled z vrcholu.", RouteType.SPORT),
            route(ostrov, "Bílý sen", "6c+", 28, 16, "Převislá cesta s krásnou linií. Fyzicky náročný start.", RouteType.SPORT),
            route(ostrov, "Magistrála", "7b", 30, 17, "Stěžejní cesta sektoru. Komplex přesné techniky a síly.", RouteType.SPORT),
            route(ostrov, "Severní varianta", "7c", 32, 18, "Krajní linie s velmi tuhým klíčovým místem. Jeden z nejtěžších v oblasti.", RouteType.SPORT)
        ))
    }

    // ── Labské pískovce ───────────────────────────────────────────────────────

    private suspend fun seedLabskePiskovce() {
        val areaId = areaDao.insert(
            AreaEntity(
                name = "Labské pískovce",
                country = "CZ",
                region = "Ústecký kraj",
                description = "Pískovcové věže na česko-saské hranici. Tradiční lezení bez mechanického jištění.",
                rockType = "Pískovce",
                latitude = 50.879,
                longitude = 14.218
            )
        )

        val rathener = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "Rathener Wände",
                description = "Klasické pískovcové věže s dlouhou tradicí. Lezení výhradně na smyčky a pískovcové uzly.",
                approach = "Z Hřenska 45 min po turistické cestě do Mezní Louky."
            )
        )

        routeDao.insertAll(listOf(
            route(rathener, "Alter Weg", "V", 20, null, "Historická cesta prvovýstupu. Klasická obtížnost.", RouteType.TRAD, GradeSystem.UIAA),
            route(rathener, "Nordkante", "VIIa", 18, null, "Exponovaná hrana s výbornými výhledy.", RouteType.TRAD, GradeSystem.UIAA),
            route(rathener, "Direktissima", "VIIIa", 22, null, "Přímá linie středem stěny. Technicky náročné.", RouteType.TRAD, GradeSystem.UIAA),
            route(rathener, "Westkante", "VIb", 16, null, "Příjemná linie se solidním jištěním.", RouteType.TRAD, GradeSystem.UIAA)
        ))

        val schrammsteine = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "Schrammsteine",
                description = "Impozantní pískovcové hřebeny s výbornými liniemi všech obtížností.",
                approach = "Z Bad Schandau 60 min po červené turistické značce."
            )
        )

        routeDao.insertAll(listOf(
            route(schrammsteine, "Ostkante", "VIb", 30, null, "Dlouhá věžovitá hrana, jedno z nejkrásnějších lezení oblasti.", RouteType.TRAD, GradeSystem.UIAA),
            route(schrammsteine, "Südwand", "IXa", 28, null, "Nejtěžší cesta hřebene. Výborný rock.", RouteType.TRAD, GradeSystem.UIAA),
            route(schrammsteine, "Normalweg", "IV", 20, null, "Přístupová cesta pro turisty. Vhodné i pro začátečníky.", RouteType.TRAD, GradeSystem.UIAA)
        ))
    }

    // ── Ještěd ────────────────────────────────────────────────────────────────

    private suspend fun seedJestedy() {
        val areaId = areaDao.insert(
            AreaEntity(
                name = "Ještědský hřbet",
                country = "CZ",
                region = "Liberecký kraj",
                description = "Čedičové a ryolitové skály s krásným výhledem na Liberec a Jizerské hory.",
                rockType = "Ryolit/čedič",
                latitude = 50.724,
                longitude = 14.995
            )
        )

        val hlavniSektor = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "Hlavní sektor",
                description = "Kompaktní stěna s cestami středních obtížností. Ideální pro tréning.",
                approach = "Z parkoviště pod Ještědem 5 min po značené cestě vpravo."
            )
        )

        routeDao.insertAll(listOf(
            route(hlavniSektor, "Sluneční terasa", "5b", 14, 8, "Příjemná cesta s výbornými chyty. Vhodná pro začátečníky.", RouteType.SPORT),
            route(hlavniSektor, "Podzimní varianta", "6a+", 16, 10, "Technická linie s klíčovým přechodem vlevo.", RouteType.SPORT),
            route(hlavniSektor, "Zimní linie", "7a", 18, 11, "Tuhé lezení v levé části stěny. Suché i za deště.", RouteType.SPORT),
            route(hlavniSektor, "Jaro na ještědu", "6b+", 17, 10, "Oblíbená cesta s krásným výhledem z vrcholu.", RouteType.SPORT)
        ))

        val suchodolskySektor = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "Suchý důl",
                description = "Menší sektor se skrytými cestami. Méně frekventovaný.",
                approach = "Z hlavního sektoru 10 min dál po lesní cestě."
            )
        )

        routeDao.insertAll(listOf(
            route(suchodolskySektor, "Skrytá linie", "6c", 15, 9, "Nenápadná cesta s překvapivě tuhým klíčovým místem.", RouteType.SPORT),
            route(suchodolskySektor, "Tichá voda", "5c+", 12, 7, "Klidná cesta pro rozcvičení. Dobrý rock.", RouteType.SPORT)
        ))
    }

    // ── Řež ───────────────────────────────────────────────────────────────────

    private suspend fun seedRez() {
        val areaId = areaDao.insert(
            AreaEntity(
                name = "Řež",
                country = "CZ",
                region = "Středočeský kraj",
                description = "Buližníkové skály nad Vltavou severně od Prahy. Mix sportovního lezení (V. masiv, Barunka) a tradičního (IV. masiv). Dostupné vlakem z Prahy za 20 min.",
                rockType = "Buližník",
                latitude = 50.1776,
                longitude = 14.3584
            )
        )

        // ── V. masiv (5. masiv) ──────────────────────────────────────────────

        val vMasiv = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "V. masiv",
                description = "Sportovní sektor přímo u trati. Dvě kolmé stěny, 10m cesty od II do 8+. Ideální pro první výlety ze stěny ven.",
                approach = "Od nádraží Řež 2 min po kolejích, skály vpravo u trati."
            )
        )

        routeDao.insertAll(listOf(
            route(vMasiv, "Schody do nebíčka", "2", 10, 4, "Snadná cesta pro rozcvičení.", RouteType.SPORT),
            route(vMasiv, "Operní", "II", 10, 4, "Klasická lehká linie.", RouteType.SPORT, GradeSystem.UIAA),
            route(vMasiv, "Operní - var.", "III+", 10, 4, "Těžší varianta nástupu.", RouteType.SPORT, GradeSystem.UIAA),
            route(vMasiv, "Hranka", "3", 10, 4, "Příjemná cesta hranou.", RouteType.SPORT),
            route(vMasiv, "Tařice", "IV", 10, 4, "Tradiční obtížnost, dobrý footwork.", RouteType.SPORT, GradeSystem.UIAA),
            route(vMasiv, "Labutí píseň", "5", 10, 4, "Plynulá cesta středem.", RouteType.SPORT),
            route(vMasiv, "Migréna", "V", 10, 4, "Technická cesta na šikmých chytech.", RouteType.SPORT, GradeSystem.UIAA),
            route(vMasiv, "Vyhřežlá spára", "V", 10, 4, "Spárová linie, jamky a ostrůvky.", RouteType.SPORT, GradeSystem.UIAA),
            route(vMasiv, "Ukrajinská", "5", 10, 4, "Plynulá cesta s dobrým jištěním.", RouteType.SPORT),
            route(vMasiv, "Mesrova rozcvička", "6+", 10, 4, "Dobrá rozcvičovací cesta.", RouteType.SPORT),
            route(vMasiv, "Cesta do Valhally", "6+", 10, 4, "Příjemná sportovní linie.", RouteType.SPORT),
            route(vMasiv, "Batman", "7-", 10, 4, "Krátký tuhý úsek v polovině.", RouteType.SPORT),
            route(vMasiv, "Pětka", "7-", 10, 4, "Technická cesta na drobných chytech.", RouteType.SPORT),
            route(vMasiv, "Y man", "7", 10, 4, "Silová linie s dynamickým pohybem.", RouteType.SPORT),
            route(vMasiv, "Spiderman", "7", 10, 4, "Kompaktní buližník, tuhé klíčové místo.", RouteType.SPORT),
            route(vMasiv, "Superjanek", "7", 10, 4, "Pohybová cesta s pěkným závěrem.", RouteType.SPORT),
            route(vMasiv, "Hrana profesora Zádrhela", "7", 10, 4, "Exponovaná hrana, skvělý pohled na Vltavu.", RouteType.SPORT),
            route(vMasiv, "Plotýnka", "7+", 10, 4, "Šikmé plotýnky, minimální tření.", RouteType.SPORT),
            route(vMasiv, "Dirty Boy", "8+", 10, 4, "Silová přepadlá cesta.", RouteType.SPORT),
            route(vMasiv, "Superman", "8+", 10, 4, "Nejtěžší cesta sektoru, tuhý přelis.", RouteType.SPORT)
        ))

        // ── Barunka ──────────────────────────────────────────────────────────

        val barunka = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "Barunka",
                description = "Korunní klenot Řeže. Pevný buližník, 15m cesty od 5 do 8+. Většina cest na sedmičkách, husté jištění. Přístup fixním lanem.",
                approach = "Od V. masivu cca 5 min nahoru po stezce doprava, závěr fixním lanem."
            )
        )

        routeDao.insertAll(listOf(
            route(barunka, "Ryzec na hřibu", "5-", 15, 6, "Nejlehčí cesta sektoru, dobrý nástup.", RouteType.SPORT),
            route(barunka, "Prašivka", "5", 15, 6, "Příjemná technická cesta.", RouteType.SPORT),
            route(barunka, "Po stopách Franty Mrvíka", "6-", 15, 6, "Pohybová linie s dobrými chyty.", RouteType.SPORT),
            route(barunka, "Mladší ségra sl. Honold", "6", 15, 6, "Plynulá cesta středem.", RouteType.SPORT),
            route(barunka, "Enterprise", "6", 15, 6, "Klasika Barunky, sdílí první dva borháky s Barunkou.", RouteType.SPORT),
            route(barunka, "Satan reloaded", "6", 15, 6, "Kompaktní linie, chyty jen z jedné strany.", RouteType.SPORT),
            route(barunka, "Barunka", "6+", 15, 6, "Jmenovitá cesta oblasti. Technická, sdílí nástup s Enterprise.", RouteType.SPORT),
            route(barunka, "Výzva slečny Honoldové", "7-", 15, 6, "Příjemná obtížnost, pohybová.", RouteType.SPORT),
            route(barunka, "Covid", "7-", 15, 6, "Varianta Koronaviru.", RouteType.SPORT),
            route(barunka, "Bez roušky to nelez!", "7", 15, 6, "Vertikální linie na bočácích. Var. 7- vlevo.", RouteType.SPORT),
            route(barunka, "Ruce do džusovače", "7", 15, 6, "Silová cesta s tuhým klíčovým místem.", RouteType.SPORT),
            route(barunka, "Koronavirus", "7", 15, 6, "Populární cesta, kompaktní pohyb.", RouteType.SPORT),
            route(barunka, "Tuleníčko", "7+", 15, 6, "Přepadlá linie, fyzicky náročný závěr.", RouteType.SPORT),
            route(barunka, "Anděl co dělá neplechu", "8", 15, 6, "Technicky obtížná cesta pravé části.", RouteType.SPORT),
            route(barunka, "Neshoďto!", "8", 15, 6, "Silová přepadlá linie.", RouteType.SPORT),
            route(barunka, "Čuftěna", "8+", 15, 6, "Nejtěžší cesta Barunky.", RouteType.SPORT)
        ))

        // ── IV. masiv ────────────────────────────────────────────────────────

        val ivMasiv = sectorDao.insert(
            SectorEntity(
                areaId = areaId,
                name = "IV. masiv",
                description = "Největší masiv v Řeži, 40m vysoký. Tradiční lezení bez vrtaného jištění — výhradně klíny, friédy, smyčky a staré nitovky. 62 cest od II do VIII+.",
                approach = "Od nádraží Řež 5 min do svahu, masiv vysoko vpravo."
            )
        )

        routeDao.insertAll(listOf(
            // Lehké (II–III+)
            route(ivMasiv, "Školní", "II", 15, null, "Vstupní cesta pro začátečníky. Přímá linie v pravé části.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Společná", "III", 20, null, "Oblíbená klasika pro skupiny. Solídní chyty.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Koutová", "III", 20, null, "Koutová linie, dobrá pro nácvik tradičního jištění.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Kladívko", "III", 20, null, "Krátká přímá cesta.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Soňa", "III", 20, null, "Lehká tradiční cesta levou částí stěny.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Chromá", "III+", 28, null, "Levý zářez od paty do vrcholu. Název napsán na skále. Prvovýstup Pechouš & Kolář, duben 1972.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Fyzminutka", "III+", 20, null, "Krátká technická linie pro rozcvičení.", RouteType.TRAD, GradeSystem.UIAA),
            // Střední (IV–IV+)
            route(ivMasiv, "Alešova cesta", "IV", 28, null, "Klasická cesta středem masivu.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Klečkatá", "IV", 28, null, "Paralelní linie k Alešově, lehce vlevo.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Liška Bystrouška", "IV", 25, null, "Jemná technická linie.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Dvě kočky", "IV", 20, null, "Krátká linie se dvěma výraznými úchyty.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Michalova", "IV", 20, null, "Přímá linie dobré kvality.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Akátová", "IV", 20, null, "Cesta poblíž akátů v nástupu.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Strmá", "IV", 35, null, "Výrazný pilíř s horizontálními římsami a strmým koutem. Spust 25m z velkého kruhu. Prvovýstup Pechouš & Rigel, 1976.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Kůň", "IV+", 35, null, "Nástup v hlubokém zářezu bez stupů — lezec nasedá jako na koně. Spust sdílí se Strmou. Prvovýstup 1964.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Samotářská", "IV+", 30, null, "Osamělejší linie v pravé části.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Tuňák v oleji", "IV+", 20, null, "Kratší cesta s výrazným klíčovým místem.", RouteType.TRAD, GradeSystem.UIAA),
            // Těžší (V–V+)
            route(ivMasiv, "Guáno", "V", 32, null, "Zářez začínající asi ve třetině výšky. Fyzicky náročnější nástup.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Zábavný koutek", "V", 40, null, "Výrazný kout se starým nitonem pod přepisem. Jedna z nejdelších cest masivu.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Novoroční", "V+", 25, null, "Prvovýstup na Nový rok. Kompaktní technická linie.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Velká stolice", "V+", 40, null, "Výrazná linie na stolici v horní části. Varianty: Tasemnice, narovnání stolice (VI-), převisová var. (VII).", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Zelená", "V+", 30, null, "Přímá linie se zelenou patikou.", RouteType.TRAD, GradeSystem.UIAA),
            // Velmi těžké (VI–VII+)
            route(ivMasiv, "Vzpomínka na Rozsutec", "VI", 20, null, "Krátká ale intenzivní linie, inspirovaná tatranskou klasikou.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Červnová", "VI", 20, null, "Tuhá technická cesta letní obtížnosti.", RouteType.TRAD, GradeSystem.UIAA),
            route(ivMasiv, "Vibrátor", "VII+", 20, null, "Jedna z nejtěžších cest masivu. Tuhý klíč na kompaktním buližníku.", RouteType.TRAD, GradeSystem.UIAA)
        ))
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun route(
        sectorId: Long,
        name: String,
        grade: String,
        length: Int,
        bolts: Int?,
        description: String,
        type: RouteType = RouteType.SPORT,
        gradeSystem: GradeSystem = GradeSystem.FRENCH
    ) = RouteEntity(
        sectorId = sectorId,
        name = name,
        grade = grade,
        gradeSystem = gradeSystem.name,
        type = type.name,
        length = length,
        bolts = bolts,
        description = description
    )
}

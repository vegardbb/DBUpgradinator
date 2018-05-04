# Programmeringskladd
Tanker om programmeringen og modelleringen rundt masteroppgaven

## Stikkord
* Språk: Java
* Type: NoSQL
* Basert på: Voldemort
* Status: Arkitektur av testapplikasjon uferdig

**Notabene**: Man vil forsøke å implementere en modulær dataevolusjonsløsning som kjører 'oppå' databaseklienten, det vil si at programmet er "klynge-ignorant" akkurat som den jevne webapplikasjon.

## Klasser som må implementeres:
* `package voldemort.tools` (ekstra)
  * En fin idé hadde vært å implementere et konsollbasert spørregrensesnitt eller et annet Admin-verktøy der man kan operere på det implisitte dataskjemaet ved å definere endringer i et ordnet format, noe KVolve savner i sin proof-of-concept-implementasjon
* `package voldemort.dbupgradinator`
  * AbstractAggregateTransformer - en abstrakt klasse med en tilstandsvariabel som indikerer applikasjonsversjonen oppgraderingsklassen opererer på og en som indikerer nøkkelverdien til den neste applikasjonen som den transformerte verdien gjelder for; klassen har én abstrakt metoder, som implementeres i subklassen programmert og kompilert til `.class`-fil av den individuelle applikasjonsprogrammerer. Metodens navn er `TransformAggregate`, som påkalles når en forespørsel fra webapplikasjonens gamle versjon (som er under rullerende oppgradering) tilsendes databaseklienten, den påkalles både når klienten mottar data fra datalageret, etter at en `InconsistencyResolver` har flettet divergerende elementer; argumenter: key (fra DB), value (deserialisert), så vel som når klienten mottar data fra applikasjonen, altså fra forespørselen direkte
  * AppVersionResolver - hjelperklasse som kopler dataobjektets nøkkel i en innkommende HTTP-spørring (k) med applikasjonsversjonens nøkkel (x) på formen `k + ":" + x`
  * AggregateTransformerReceiver - frittstående prosess som ikke er del av en HTTP-spørrings livsløp, men som både mottar AggregateTransformer-objekter og holder rede på dem i versjonsrekkefølge i en privat liste. Har også ansvar for å påkalle transformasjonsmetoden til hvert objekt.

* `package voldemort.client`
  * DBUpgradinatorStoreClient - Separat databaseklient som importerer de nye klassene fra DBUpgradinator - pakken
  * DBUpgradinatorStoreClientFactory (?)

Alternativt kan man benytte RMI - Remote Method Invocation, det vil si at hver enkelt databaseklient kontakter en kjørende "oppgraderingskoordinator".

# Om det fysiske perspektiv av testarkitekturen
 * Hver enkelt av de fire applikasjonsnodene gjestes av DigitalOcean, og alle er spredd utover fire forskjellige datasentre i London, Amsterdam (to separate datasentre), og Frankfurt. Dette gjør at hver enkelt forespørsel til tjener kan gå omtrent like raskt med det gitte replikeringsskjema.
 * Replikeringsmønstre, 1 node per senter
   * AMS1 -> AMS1; LON1; FRA1
   * AMS2 -> AMS2; LON1; FRA1
   * LON1 -> LON1; AMS1; AMS2
   * FRA1 -> FRA1; AMS1; AMS2

## Testprossess
 * Total størrelse på testdata: 100 GB
 * Partisjon på hver instans: 25 GB; Replikert datavolum på hver instans: 50 GB; Volum per maskin: *75 GB*
 * Gjenværende volum reservert OS og hurtigminne: 80 GB (SSD-plass per droplet) - 75 GB = 5 GB
 * 1000 KB = 1 GB
 * Antakelse: Hvert aggregat i datamodell opptar i maksimalt 1 KB lagringsplass

## Trivia (Harry Potter-tema)

| Droplet | ID, apptjener | ID, DB-klient | ID, DB-tjener    |
|---------|---------------|---------------|------------------|
| LON1    | a             | avery         | riddle-diary     |
| AMS1    | b             | black         | slytherin-locket |
| AMS2    | c             | crabbe        | rawenclaw-diadem |
| FRA1    | d             | dolohov       | nagini           |

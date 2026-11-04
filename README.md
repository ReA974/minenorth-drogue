# MineNorth Drogue

Mod Forge 1.20.1 (`minenorthdrogue`), dépend de `minenorthapi` (protection par le Core). Remplace Narcosis : marijuana, cocaïne, méthamphétamine, avec production et lien avec le mod Police.

## Production
- **Tentes de culture** (2 blocs de haut) : **1 × 2** (`grow_tent_long`, 2 plantes, dans le sens où regarde le joueur) et **1 × 1** (`grow_tent_small`, 1 plante). La tente 2 × 2 a été retirée (celles déjà posées restent et rendent 2 tentes 1 × 2 à la casse), posées vers le sud-est de la case visée. Cannabis et coca ne poussent que là ; l'éphédra pousse aussi sous tente (en plus du plein champ).
  - **Ouvrir / fermer** : clic droit **à main nue sur le haut** de la tente. Fermée : toile noire opaque (on ne voit pas l'intérieur) ; ouverte : filet transparent, on accède aux plantes.
  - Tente ouverte, clic droit sur une case du bas avec des graines pour planter, à main nue sur une plante mûre pour récolter (feuilles + parfois une graine).
  - **Lampe UV** (`uv_lamp`, clic droit sur la tente) : la pousse est deux fois plus rapide ; accroupi, main vide : la retirer.
- **Arrosoir infini** (`watering_can`, 4 lingots de fer + un seau d'eau) : un clic droit arrose toutes les plantes de la tente (ouverte) sans jamais se vider ; il se penche en versant (animation de ~1 s : 3 modèles inclinés choisis par le prédicat `minenorthdrogue:pour`) et de l'eau tombe sur les plantes.
- **Arrosage** : la terre de la tente doit être humide pour que la plante pousse. Clic droit avec un **seau d'eau** (3 stades, rend le seau vide) ou une **bouteille d'eau** (1 stade, rend la bouteille vide) sur une case du bas ; chaque stade de pousse consomme 1 d'eau. La terre devient plus sombre quand elle est humide.
- **Éphédra** (`ephedra_seeds`) : pousse en plein champ sur terre labourée (4 stades).
  Les graines ne se craftent pas : le staff les donne (créatif / commande) ou les vend via le mod Boutiques.
- **Produit chimique** (`chemical`) : poudre à canon + poudre d'os + fiole.
- **Joint** : 3 feuilles de cannabis + papier → 2 joints (établi).
- **Table à rouler** (`roll_table`, 1 bloc, 3 planches de chêne noir + 4 bâtons + 1 papier en établi) : clic droit avec une pile de **feuilles de cannabis** ou de **papier** pour les déposer ; elle roule 2 joints toutes les 4 s (3 feuilles + 1 papier, réglage `secondesParLot`), à récupérer à main nue (accroupi, main vide : tout reprendre). Plateau de roulage, broyeur, paquet de feuilles et machine à rouler en 3D.
- **Cocaïne** : 4 feuilles de coca + 1 produit chimique **au laboratoire** = 6 **pâtes de coca** ; la pâte se cuit dans le **Four de cuisson** (`cooking_oven`, 1 bloc, 8 lingots de fer + 1 haut fourneau) : pâte déposée, main vide pour lancer, 1 pâte = 1 poudre, 30 s (réglages `[four]`). Aucune recette d'établi ni de four vanilla pour la pâte ou la poudre.
- **Laboratoire de chimie** (`lab`, multibloc 2 de large × 1 de profondeur × 2 de haut, face tournée vers le joueur) : clic droit pour y poser les ingrédients un à un ; la fabrication démarre quand la recette est complète, le produit se récupère à main nue (accroupi, main vide : reprendre les ingrédients).
  - Cristaux de méthamphétamine : 3 éphédra + 1 produit chimique, 45 s, 6 unités.
- **Table d'ensachage** (`pack_table`, **1 × 2** : 2 blocs côte à côte à droite du joueur, face vers lui ; 4 planches + 4 lingots de fer en établi) : clic droit avec une pile de poudre, de cristaux ou de sachets vides pour les déposer ; elle fait un sachet toutes les 3 s (3 unités + 1 sachet vide), à récupérer à main nue (accroupi, main vide : tout reprendre). Réglage : `secondesParSachet`.
- **Emballage à la main** (établi, sans table) : sachet vide = 2 papiers + 1 ficelle (4 sachets) ; 3 poudres de cocaïne + 1 sachet vide = **Sachet de cocaïne** ; 3 cristaux de meth + 1 sachet vide = **Sachet de méthamphétamine**. Ce sont les sachets que l'on consomme.

## Effets (positifs, limités par un délai d'attente)
| Substance | Effets par défaut | Délai avant la dose suivante |
|---|---|---|
| Joint | Régénération I, 20 s | 2 min |
| Cocaïne | Vitesse I + Hâte I, 60 s | 4 min |
| Méthamphétamine | Vitesse II + Hâte II + Résistance I, 45 s | 6 min |

Tout est réglable dans `config/minenorth_drogue.toml` (serveur dédié) : lignes `effet;secondes;niveau`, délais, durées et rendements du laboratoire.

## Lien avec le mod Police
Chaque prise est notée dans les données du joueur (`minenorthpolice_drogues`, format lu par le test salivaire du mod Police) : le test salivaire est positif pendant `drogue_detection_minutes` et affiche le nom de la substance (Joint, Cocaïne, Méthamphétamine).

## Compiler
Java 17 (Gradle 8.8 ne tourne pas sur un JDK trop récent : `JAVA_HOME` sur un JDK 17 à 21), puis `./gradlew build`.

Objets refaits en 3D plus détaillés : **sachet de cocaïne** (sachet zip transparent, poudre tassée, étiquette), **poudre de cocaïne** (tas sur papier avec cristaux et carte), **joint** (filtre carton, cône de papier, torsade, braise), sachet de meth, cristaux de meth, sachet vide (palette `palette2.png`).

Tous les objets et blocs sont des modèles 3D (modèles JSON à base de boîtes, palette de couleurs `textures/block/palette.png`).

Onglet créatif **Drogue** (icône : joint). Tous les textes sont en français (`fr_fr` et `en_us` identiques).

## Animations de prise (13/10)
- **Joint** : on le fume (le joint monte à la bouche, bras levé, fumée et crépitement, allumage au début).
- **Sachet de cocaïne** : pendant la prise, l'objet devient un miroir avec deux lignes et un billet roulé ; le miroir se lève devant le visage et glisse le long des lignes, qui disparaissent une à une (bruits de reniflement, petit nuage de poudre).
- **Sachet de méthamphétamine** : pendant la prise, l'objet devient une seringue qui part vers l'avant-bras ; le piston s'enfonce (petit bruit de piqûre).
Poses et mouvements : `client/DrugClient.java` ; modèles changeants : prédicats `minenorthdrogue:snort` / `inject` (`ClientSetup`).

## Mini-jeux, lots du labo, récolte (14/10)
- **Mini-jeu de timing** (ensachage et roulage) : sur la table, main vide = lancer ; une barre apparaît au-dessus de la barre d'objets avec une **zone verte** et un curseur qui va et vient ; clic droit sur la table pour l'arrêter. Dans la zone = manche réussie. Jusqu'à 5 manches (ensachage) ou 6 (roulage), curseur lent, un peu plus rapide à chaque manche, zone plus étroite à partir de la 4e.
  - Ensachage : manche réussie = 1 sachet (3 poudres / cristaux + 1 sachet vide) ; ratée = sachet déchiré (on perd le sachet vide et 1 unité). Toutes les manches réussies (3 ou plus) = 1 sachet en bonus.
  - Roulage : réussie = 2 joints (3 feuilles + 1 papier) ; ratée = 1 joint mal roulé. Toutes réussies = 1 joint en bonus.
- **Table à rouler 1 × 2** (comme la table d'ensachage : 2 blocs côte à côte, face vers le joueur).
- **Labo : plusieurs lots d'un coup** : on y dépose les ingrédients en plus grande quantité (jusqu'à `lotsMax` lots, 4 par défaut), puis **main vide** pour lancer. Produit multiplié par le nombre de lots, durée +50 % par lot en plus.
- **Récolte** : une plante mûre sous tente donne `feuillesMin` à `feuillesMax` feuilles (6 à 10 par défaut) ; l'éphédra de plein champ en donne 4 à 8.

- **Terreau** (`potting_soil`, 2 terre + 1 poudre d'os = 2 terreaux) : à poser dans le pot de la tente avant de planter et d'arroser. Pot refait en 3D (soucoupe, rebord, terreau sec/humide avec perlite et étiquette). Récolter vide le pot : il faut remettre du terreau.
- **Méthamphétamine** : plus d'essence, 3 éphédra + 1 produit chimique au labo.
- **Animation du labo** pendant la fabrication : flamme sous le ballon, bulles colorées (bleu pour la meth, beige pour la pâte), fumée du condenseur, bruits de chimie.

## Labo et banc d'ensachage en 3D animée (17/10)
- **Laboratoire** : nouveaux modèles (meuble, hotte, tuyauterie, manomètre, plaque chauffante, ballon, condenseur, erlenmeyer, cuve inox). Propriété de bloc `anim` (0 à 3, ajoutée à `lab` et `pack_table`, mêmes ids : sauvegardes compatibles) : pendant une fabrication, `LabBE.serverTick` change l'image toutes les 4 ticks (flamme, bulles, goutte, aiguille, voyant, vapeur) ; retour à 0 dès que la fabrication s'arrête.
- **Table d'ensachage** : nouveaux modèles (balance à fléaux, bocal, coupe de cristaux, soudeuse, tas de sachets). L'animation suit le mini-jeu : `PackBlock.startAnim` puis ticks planifiés tant que `MiniGame.activeAt(table)` ; un tick aléatoire remet au repos une table restée animée après un redémarrage.
- Texture `labo_set.png` (palette 4 × 4 px + tuiles de motifs). Modèles générés par script (un modèle par partie et par image, dédoublonnés) ; non testé en jeu.

## Écrans des machines, héroïne, 3D réaliste (28/10)
- **Écrans** (charte MineNorth) pour le **laboratoire**, le **four**, la **table d'ensachage** et la **table à rouler** : on dépose les ingrédients dans des emplacements, bouton Lancer / Cuire, progression affichée, produit dans un emplacement de sortie. Le **mini-jeu** de timing se joue dans l'écran (bouton Stop ou Espace, petite tolérance de latence) ; emplacements verrouillés pendant la partie ; « Table occupée » si un autre joueur joue. Les anciennes sauvegardes (compteurs) sont reprises dans les emplacements, le surplus tombe au sol. Code : `menu/*`, `client/*Screen`, `Stations`, `MiniGame.view`.
- **Tente** : pas d'écran, tout se fait objet en main (terreau, graines, seau / bouteille / arrosoir, lampe UV ; main nue sur le haut = ouvrir / fermer, sur une plante mûre = récolter).
- **Héroïne** : graines de pavot (`poppy_seeds`, données par le staff / boutiques) plantées sous tente → capsules de pavot (`poppy_pod`) ; au labo 4 capsules + 1 produit chimique = 6 héroïne (poudre) ; table d'ensachage (emplacement « Héro ») ou établi : 3 poudres + 1 sachet vide = **Sachet d'héroïne**, pris en injection (seringue). Réglages `[heroine]` : effets, délai (420 s), durée labo (50 s), rendement (6).
- **3D** : tentes en toile noire texturée, intérieur réfléchissant (mylar), armature métal ; pots en tissu sur soucoupe, terreau sec / humide avec perlite ; plantes refaites (feuilles de cannabis dentelées et têtes, coca à baies rouges, éphédra articulée, pavot : rosette, fleurs rouges puis capsules).

- **Mini-jeu refait (28/10)** : le curseur avance à l'horloge (millisecondes) et non plus aux ticks du serveur, sur le modèle du vol d'animaux (`PetSteal`) : zone, vitesse et phase de la manche passent par `MiniGame.view`, l'écran lance son chrono à la réception de la manche, fige le curseur à l'appui et envoie la position vue (`GameMenu.BTN_SEEN` + 0 à 100), que le serveur accepte si son propre curseur y est passé dans la dernière seconde. Vitesse 0,75 à 1,3 traversée/s, zone 26 % puis 20 % puis 16 %.


## Qualité, coupe, clients, nouvelles drogues, santé (29/10)
- **Qualité** (`Quality`, NBT `mn_q`, 1 à 5, 3 par défaut) : à la récolte 2 + lampe UV 1 (LED 2) + engrais 1 (champignons : 3 + engrais). Labo : qualité de la plante, -1 à partir de 3 lots. Four, ensachage, roulage, presse : conservée. Séchoir : +1. Coupe : -1. Effets × (0,5 + q/6), prix × (0,4 + 0,2 q). Les sorties des machines n'empilent que la même qualité (`Stations.addOutput/room` avec modèle).
- **Tente sans nouvelles propriétés** (chaque propriété multiplie les états) : engrais = `LAMP` sur une case du bas, LED = `SOIL` sur une case du haut, goutte-à-goutte = `WATER > 0` sur une case du haut (`TentBlock.fert/led/drip`). Champignons : `Plant.MUSHROOM`, ne poussent que tente fermée sans lampe.
- **Table de coupe** (`CutBE/CutMenu/CutScreen`) : 2 doses + 1 additif = 3 doses, -1 étoile, additif noté dans `mn_cut` (2 au plus) ; effets bonus dans `DrugItem.finishUsingItem`.
- **Clients** (`Customers`, `CustomerEntity`, `DrogueNet`, app MRPhone « Deal » (ex-« Plans », renommée le 02/11) `client/PhoneDeals` + `PhoneDealsScreen`) : demandes générées selon le temps, rendez-vous à 60–180 blocs, client PNJ (skins par défaut) qui apparaît à 40 blocs, paiement `MineNorth.bank().refund(..., "drogue:vente")`, bonus qualité, fidélité sauvegardée (`minenorthdrogue_clients`), alerte police aléatoire. L'icône n'apparaît qu'après avoir eu de la drogue sur soi une première fois (`DealerFlag`, gardé dans les données du joueur). Icône ajoutée au téléphone patché (`minenorth-phone/tools/patch` : `DEALS`, `JobsHook`, `deals.png`).
- **Nouvelles drogues** : champignons (tente), ecstasy (`sassafras_oil` → labo `mdma_crystals` → `PressBE` mini-jeu), LSD (`Ergot` sur le blé → labo `lsd_liquid` → table à rouler, 4 buvards par fiole).
- **Odeur et police** (`Odor`) : volutes vertes (tente qui pousse, labo actif, séchoir chargé) sauf `carbon_filter` à 4 blocs ; chiens K9 (`minenorthpets_k9`) qui marquent l'arrêt (blocs à 7, joueurs à 4) ; alertes `alertPolice` / `alertSecours`.
- **Accidents de labo** : 3 lots ou plus sans `air_extractor` à 5 blocs → `labo.incendiePourcent` (15 % par défaut) de chance d'explosion sans dégâts de blocs + feu, un quart du produit perdu, secours prévenus.
- **Santé** (`Addiction`, `DrugEffects`, `client/DrugVisuals`) : dépendance (0–100, -1/min), manque, overdose (mélange de deux drogues dures en 3 min : une chance sur trois, héroïne 5 étoiles sans accoutumance, dépendance extrême), naloxone (`NaloxoneItem`) ; sans elle, dégâts magiques que le mod Secours transforme en coma. Filtres visuels : shaders post vanilla (phosphor, desaturate, color_convolve, wobble, deconverge, blur).
- Compilation : `libs/mrphone-0.9.4.jar` (copie du téléphone d'origine, `compileOnly`, non redistribuée) et `../minenorth-api/build/libs`.
- **Ventilation v2** (`gen_vent.py` du scratchpad) : filtre à charbon cylindrique à 16 faces (tôle perforée, brides, gaine alu) ; extracteur en ventilateur de gaine avec hélice animée (`ExtractorBE` sans données + `client/ExtractorRenderer`, modèle additionnel `block/air_extractor_blades`).

## Prime de risque police et clients fidèles (04/11)

- **Prime de risque** : plus il y a de policiers connectés (en service par défaut), plus chaque vente aux clients rapporte. Multiplicateur = 1 + min(`bonusMax`, policiers × `bonusParPolicier`) / 100, réglable dans `[police]` de `minenorth_drogue.toml` (défaut : +10 % par policier, plafond +100 %, `seulementEnService = true`). Le bandeau en haut de l'appli Deal affiche le multiplicateur actuel et le nombre de policiers comptés ; le message de paiement détaille la prime.
- **Clients fidèles** : chaque client a une fiche par dealer (niveau de fidélité 0 à 5 : Nouveau, Habitué, Fidèle, Client VIP ; nombre d'achats ; total dépensé ; dernier achat), sauvegardée (`minenorthdrogue_clients`, l'ancien format est repris). Onglet « Clients fidèles » dans l'appli Deal, les plus fidèles d'abord.
- **Bonus de fidélité** : en plus du prix et des quantités qui montent avec la fidélité, les clients à 3 cœurs et plus reviennent plus souvent (40 % des nouvelles demandes).
- Le réseau passe en version 2 : client et serveur doivent avoir le même jar.

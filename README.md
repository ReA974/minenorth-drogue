# MineNorth Drogue

Mod Forge 1.20.1 (`minenorthdrogue`), sans dépendance. Remplace Narcosis : marijuana, cocaïne, méthamphétamine, avec production et lien avec le mod Police.

## Production
- **Tentes de culture** (2 blocs de haut) : **2 × 2** (`grow_tent`, 4 plantes), **1 × 2** (`grow_tent_long`, 2 plantes, dans le sens où regarde le joueur) et **1 × 1** (`grow_tent_small`, 1 plante), posées vers le sud-est de la case visée. Cannabis et coca ne poussent que là ; l'éphédra pousse aussi sous tente (en plus du plein champ).
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
- **Laboratoire de chimie** (`lab`, multibloc 2 de large × 1 de profondeur × 2 de haut, face tournée vers le joueur) : clic droit pour y poser les ingrédients un à un ; la fabrication démarre quand la recette est complète, le produit se récupère à main nue (accroupi, main vide : reprendre les ingrédients).
  - Poudre de cocaïne : 4 feuilles de coca + 1 produit chimique, 30 s, 6 unités.
  - Cristaux de méthamphétamine : 3 éphédra + 1 produit chimique + 1 **essence** (`minenorth_harvest:gasoline`, du charbon si le mod Récolte est absent), 45 s, 6 unités.
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

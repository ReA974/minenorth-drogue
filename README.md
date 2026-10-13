# MineNorth Drogue

Mod Forge 1.20.1 (`minenorthdrogue`), sans dépendance. Remplace Narcosis : marijuana, cocaïne, méthamphétamine, avec production et lien avec le mod Police.

## Production
- **Tente de culture 2 × 2** (`grow_tent`, 2 de haut, posée vers le sud-est de la case visée) : cannabis et coca ne poussent que là. Clic droit sur une case du bas avec des graines pour planter,
  à main nue sur une plante mûre pour récolter (feuilles + parfois une graine). **Lampe UV** (`uv_lamp`, clic droit sur la tente) : la pousse est deux fois plus rapide ; accroupi, main vide : la retirer.
- **Arrosage** : la terre de la tente doit être humide pour que la plante pousse. Clic droit avec un **seau d'eau** (3 stades, rend le seau vide) ou une **bouteille d'eau** (1 stade, rend la bouteille vide) sur une case du bas ; chaque stade de pousse consomme 1 d'eau. La terre devient plus sombre quand elle est humide.
- **Éphédra** (`ephedra_seeds`) : pousse en plein champ sur terre labourée (4 stades).
  Les graines ne se craftent pas : le staff les donne (créatif / commande) ou les vend via le mod Boutiques.
- **Produit chimique** (`chemical`) : poudre à canon + poudre d'os + fiole.
- **Joint** : 3 feuilles de cannabis + papier → 2 joints (établi).
- **Laboratoire de chimie** (`lab`, multibloc 2 de large × 1 de profondeur × 2 de haut, face tournée vers le joueur) : clic droit pour y poser les ingrédients un à un ; la fabrication démarre quand la recette est complète, le produit se récupère à main nue (accroupi, main vide : reprendre les ingrédients).
  - Poudre de cocaïne : 4 feuilles de coca + 1 produit chimique, 30 s, 6 unités.
  - Cristaux de méthamphétamine : 3 éphédra + 1 produit chimique + 1 redstone, 45 s, 6 unités.
- **Emballage** (établi) : sachet vide = 2 papiers + 1 ficelle (4 sachets) ; 3 poudres de cocaïne + 1 sachet vide = **Sachet de cocaïne** ; 3 cristaux de meth + 1 sachet vide = **Sachet de méthamphétamine**. Ce sont les sachets que l'on consomme.

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

Tous les objets et blocs sont des modèles 3D (modèles JSON à base de boîtes, palette de couleurs `textures/block/palette.png`).

Onglet créatif **Drogue** (icône : joint). Tous les textes sont en français (`fr_fr` et `en_us` identiques).

# Gem Nest Miner

By Liam van Hoving and Diederik Verdaasdonk 

Gem Nest Miner is a Java idle miner prototype about birds collecting gems inside a mine.

## First playable idea

- A blue bird walks through a jungle mine, harvests gems, and drops them in a storage box.
- Every mine starts with a blue bird.
- Each bird has 5 walking speed levels, 5 strength levels, and 5 mining speed levels.
- The blue bird reaches level 15 when all three upgrade paths are maxed.
- Birds must be tapped to start mining until Auto Mine is bought.
- A level 15 blue bird unlocks the option to add a green bird to the same mine.
- Green birds use the same upgrade system, but start stronger and scale better.
- A strong lift bird slowly carries gems from the mine basket to the surface.
- A swift nest bird slowly moves gems across the jungle to the big nest homebase.
- The lift bird and nest runner have separate move speed, pickup speed, and capacity upgrades.
- If transport birds cannot carry every waiting gem, leftovers stay in the mine storage box.
- Gems in the homebase become the player's inventory.
- The top-left inventory uses a small chest badge instead of a large title panel.
- The screen uses stacked mine rows, a left lift tower, a surface station, a bird-owned home nest tower, and an upgrade board.
- Upgrade cards appear in a large panel on the right.
- Clicking a mine shows bird walking speed, strength, mining speed, and green bird controls.
- Clicking a new mine sign lets the player buy another mine.
- New mines and their upgrades become more expensive as the mine number rises.
- Clicking the lift bird or nest runner bird shows transport upgrades.
- Birds show a small mining timer while harvesting.
- The homebase is now a side tower so it does not overlap the mines.

## Java classes

- `Main` starts the game window.
- `GamePanel` runs the game loop, handles buttons, and draws the screen.
- `Bird` controls bird movement, harvesting, carrying, animation, and upgrades.
- `Courier` controls the strong lift bird and swift nest bird transport systems.
- `Egg` is kept for later unlock animations.
- `GemBank` stores the player's gems.
- `Mine` stores each mine row, its blue bird, optional green bird, and waiting box gems.

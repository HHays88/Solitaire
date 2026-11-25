References & Credits:
https://koor.fr/Java/Android/Solitaire/Solitaire.wp
https://www.youtube.com/watch?v=vYsG2gToAzM
https://dev.to/quantotius/building-a-klondike-solitaire-game-in-vanilla-javascript-2jg0
https://pysolfc.sourceforge.io/
https://github.com/tobiasBielefeld/Simple-Solitaire 

I compared the following tutorials’ methods and reasoning in ChatGPT to help me decide which methods I liked,
learn why these apps used their methods and some modern techniques to replace them, and reviewed suggestions 
for features I had overlooked in my original concept- such as Undo and Statistics. I ultimately left 
statistics out of the game due to time constraints but plan to add Least Moves, Fastest Time, and Highest Score 
to track individually and store in local JSON files. I would also like to return to add AutoComplete functionality.

After deciding on features and logic, I had chatGPT create the codes and I looked over them to make sure I understood
what each section was responsible for, asking questions when I wasn’t sure what it was doing and notating the what
each method was designed to do. To further ensure proper app creation, the entire process was broken up into 12 phases
to build and test the game logic before adding visuals. After every new feature was implemented, extensive JUnit tests 
were run to ensure each functionality worked together logically. Once visuals were enabled and all files could communicate
functionality such as playfield, card, and tableau rendering, tap-to-move, and score and move count incrementing I 
connected my Samsung Galaxy S25 Ultra to Android Studio and tested game functionality and visual placements, including 
adjusting ratios in card rendering to fix overlapping symbols. 

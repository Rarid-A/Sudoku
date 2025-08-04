# Sudoku (Android)

A Sudoku puzzle game for Android, built with Java and Android Studio.

## Features

- Classic Sudoku gameplay: Fill the grid until all 9 numbers are present in each row and column.
- Generator that can create an infinite amount of puzzles.
- Three difficulties, Hard, Medium and Easy
- Leaderbord to see your top 10 fastest times on each difficulty
- Light and dark themes.
- Settings menu that adds quality of life features like auto validation and hints.

## Project Structure

- `app/src/main/java/com/rarid/Sudoku/`  
  Main Java source code, including [`MainActivity.java`](app/src/main/java/com/rarid/wordle/MainActivity.java) and [`ProgressManager.java`](app/src/main/java/com/rarid/wordle/ProgressManager.java).
- `app/src/main/java/com/rarid/sudoku/generator/SudokuGenerator.java/`
  Generator that creates the puzzles.
- `app/src/main/res/layout/activity_main.xml`  
  Main UI layout.
- `app/src/main/res/values/`  
  Strings and themes.
- `app/build.gradle.kts`  
  App module Gradle build script.

## Building and Running

1. Download the apk from [https://drive.google.com/file/d/1HBNnCECKVZyMUsjYTbNSq38byccMenfK/view?usp=sharing](https://drive.google.com/file/d/1NZeCYECPyD_lpdHzlFYBjGLrDFQheunM/view?usp=sharing)
2. open it in android studio or an android device
3. Connect an Android device or start an emulator.
4. Click **Run** or open the app on the device


## How to Play

- Enter Numbers using the on-screen keyboard.
- No repeating numbers in any row, column or box.
- Try to go as fast as possible to get a good time on the personal leaderboard!

---

**Made by Rarid

# Mora Jai Box Simulator and Analysis Tools

This repository contains the supporting code for my Mora Jai Box [challenge puzzles](https://chandler.io/posts/2025/07/mora-jai-box-simulator) and [state space analysis](https://chandler.io/posts/2025/07/mora-jai-box-solution-space-analysis/).

The project is split into 3 modules that can be built with Maven and Java >= 17.

`mvn clean package` will build all 3 modules.

## morajai-core

Contains the state search tool, which generates about 600MB of data needed for analysis.  To use the `-g` argument you must have a GPU and OpenCL set up.

These arguments worked well on my Nvidia 2080 GPU (16GB RAM, 16 CPU threads) for the full 10,000 box analysis (about 3 days runtime):

```java -Xmx10g -jar morajai-core/target/morajai-core-1.0.0-SNAPSHOT.jar --skipTo=0 --storageDir=./results -g4 -G16 -c0 -C8```

The output is split into 10,000 text files which can be parsed and processed using the `morajai-analysis` module.

## morajai-analysis

 * `CopyCompleteResults.java` - Consolidate the .txt files into JSON format
 * `ResultFilter.java` - File through the JSON to extract states with the desired characteristics
 * `ResultStatistics.java` - General stats calculator
 * `ResultPreviewServer.java` - Launches a local web server that can preview the output of `ResultFilter.java`
 * `MoraJaiSolver.java` - A BFS solver that can print out the solution path for a given state.

## morajai-simulator

Contains the source code and build script that produce an embeddable JavaScript file for the simulator widget.

Emscripten `emcc` must be on the classpath to build the WASM module.

The minified JS bundle ends up here:

```morajai-simulator/target/classes/js/morajai-bundle.min.js```

It can be instantiated with this HTML (Bootstrap for GUI):

```html
<script type="module" src="./morajai-bundle.min.js"></script>

<div class="col-5 p-2 position-relative">
  <div class="mb-3 d-flex">
    <div class="form-check form-switch d-flex align-items-center">
      <input class="form-check-input" type="checkbox" id="spoilerMode">
      <label class="form-text form-check-label ms-2" for="spoilerMode">Spoiler Mode</label>
    </div>
  </div>
  <canvas id="puzzle-canvas" class="w-100 p-0"></canvas>
</div>

<script>
  function main() {
    // Load 
    const MoraJaiSimulator = window['MoraJaiSimulator'];
    if (!MoraJaiSimulator) {
      console.error("Failed to load MoraJaiSimulator class.");
      return;
    }

    const initialSpoilerMode = false;

    const onResizeCanvas = (width) => {
      console.log("onResizeCanvas", width);
      // Callback with the current width of the responsive canvas, in case you need to resize anything else
    }

    const onUpdateState = (targetColors, targetTiles, activeStickers) => {
      console.log("onUpdateState", targetColors, targetTiles, activeStickers);
      // Called every time the puzzle changes.  These args can be stored and passed back to setInitialState if needed
    }

    const onSolved = () => {
      console.log("onSolved");
      // Called when the puzzle is solved - you must then call loadPuzzle(...) to reset it
    }

    window.addEventListener('load', function() {
      morajai_box = new MoraJaiSimulator(
        "puzzle-canvas", // The canvas ID
        initialSpoilerMode,
        onResizeCanvas,
        onUpdateState,
        onSolved);
      
      document.getElementById('spoilerMode').addEventListener('click', function() {
        var spoilerMode = document.getElementById('spoilerMode').checked;
        morajai_box.setSpoilerMode(spoilerMode);
        morajai_box.render();
      });

      // Use this to preload a saved state before calling start():
      // morajai_box.setInitialState(targetColors, targetTiles, activeStickers);

      morajai_box.start(); // Call this only once

      morajai_box.loadPuzzle(["WH","WH","WH","WH","BK","BK","BK","BK","WH"], ["WH","WH","BK","WH"]);
    });
  }

  document.addEventListener('DOMContentLoaded', main);

</script>
```

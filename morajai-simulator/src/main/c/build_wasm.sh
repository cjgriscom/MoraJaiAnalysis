mkdir target || true
mkdir target/js || true

/usr/lib/emscripten/emcc \
  src/main/c/morajai.c             \
  src/main/c/emscripten_entry.c    \
  -I src/main/c/morajai            \
  -O3                              \
  -s WASM=1                        \
  -s MODULARIZE=1                  \
  -s EXPORT_ES6=1                  \
  -s SINGLE_FILE=1                 \
  -s ENVIRONMENT=web               \
  --closure=1                       \
  --minify=0                        \
  -s EXPORT_NAME=\"MoraJaiWasm\"   \
  -s EXPORTED_FUNCTIONS=['_init_from_string','_wasm_press_tile','_wasm_press_outer','_get_state'] \
  -s EXPORTED_RUNTIME_METHODS="['ccall','UTF8ToString']" \
  -o target/js/morajai_wasm.js             # morajai_wasm.js + morajai_wasm.wasm


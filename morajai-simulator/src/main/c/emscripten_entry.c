#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include <emscripten/emscripten.h>

#include "morajai.h"

/* ------------------------------------------------------------
 *  Internal helpers
 * ----------------------------------------------------------*/

// Map color strings <-> enum values
static const char *COLOR_CODES[] = {
    "GY", // C_GY
    "RD", // C_RD
    "GN", // C_GN
    "BK", // C_BK
    "WH", // C_WH
    "PI", // C_PI
    "PU", // C_PU
    "YE", // C_YE
    "BU", // C_BU
    "OR"  // C_OR
};

#define NUM_COLOR_CODES (sizeof(COLOR_CODES) / sizeof(COLOR_CODES[0]))

static color_t code_to_color(const char *code)
{
    for (size_t i = 0; i < NUM_COLOR_CODES; ++i) {
        if (strcmp(code, COLOR_CODES[i]) == 0) {
            return (color_t)i;
        }
    }
    // Default – grey, but ideally invalid input should be handled by caller
    return C_GY;
}

static const char *color_to_code(color_t c)
{
    if (c < NUM_COLOR_CODES) {
        return COLOR_CODES[c];
    }
    return "??";
}

/* ------------------------------------------------------------
 *  API wrappers exposed to JS
 * ----------------------------------------------------------*/

// init_from_string("BK,BK,BK,BK_GY,GY,GY,GY,GY,GY,BK,BK,BK_GY,GY,GY,GY,GY,GY,BK,BK,BK")
EMSCRIPTEN_KEEPALIVE
int init_from_string(const char *init_str)
{
    if (!init_str) return -1;

    char *copy = strdup(init_str);
    if (!copy) return -1;

    // Split by '_': first part is target, second part is comma-separated tiles
    char *target_part = strtok(copy, "_");
    char *tiles_part  = strtok(NULL, "_");
    char *active_part = strtok(NULL, "_");

    if (!target_part || !tiles_part || !active_part) {
        free(copy);
        return -1;
    }

    color_t target_colors[4] = {0};
    size_t idx = 0;

    char *tok = strtok(target_part, ",");
    while (tok && idx < 4) {
        target_colors[idx++] = code_to_color(tok);
        tok = strtok(NULL, ",");
    }

    if (idx != 4) {
        free(copy);
        return -1; // Not enough target colors supplied
    }

    color_t tiles[9] = {0};
    idx = 0;

    tok = strtok(tiles_part, ",");
    while (tok && idx < 9) {
        tiles[idx++] = code_to_color(tok);
        tok = strtok(NULL, ",");
    }

    if (idx != 9) {
        free(copy);
        return -1; // Not enough tile colors supplied
    }

    color_t active_colors[9] = {0};
    idx = 0;

    tok = strtok(active_part, ",");
    while (tok && idx < 9) {
        active_colors[idx++] = code_to_color(tok);
        tok = strtok(NULL, ",");
    }

    init(target_colors, tiles);
    set_active_colors(active_colors);

    free(copy);
    return 0;
}

// Directly expose the underlying functions
EMSCRIPTEN_KEEPALIVE
uint8_t wasm_press_tile(uint8_t tile)
{
    return press_tile(tile);
}

EMSCRIPTEN_KEEPALIVE
uint8_t wasm_press_outer(uint8_t outer)
{
    return press_outer(outer);
}

// Generate a comma-separated state string: tiles 0-8, then '_', then outers 0-3.
EMSCRIPTEN_KEEPALIVE
const char *get_state(void)
{
    static char buffer[128];  // Sufficient for our compact codes
    buffer[0] = '\0';

    // Tiles
    for (uint8_t i = 0; i < 9; ++i) {
        strcat(buffer, color_to_code(get_tile_color(i)));
        if (i != 8) strcat(buffer, ",");
    }

    strcat(buffer, "_");

    // Outers
    for (uint8_t i = 0; i < 4; ++i) {
        strcat(buffer, get_outer_state(i) ? "1" : "0");
        if (i != 3) strcat(buffer, ",");
    }

    return buffer;
}

// API for Mora Jai box simulator

#ifndef MORAJAI_H
#define MORAJAI_H

#include <stdint.h>
#include <stdbool.h>

#define N_COLORS 10
// Color enum
typedef enum {
	C_GY,
    C_RD,
    C_GN,
	C_BK,
	C_WH,
	C_PI,
	C_PU,
	C_YE,
	C_BU,
	C_OR,
} color_t;


// Initialize with color state
// The arrangement is:
// 0       1
//   0 1 2
//   3 4 5
//   6 7 8
// 3       2
void init(color_t target_colors[4], color_t tile_colors[9]);
void set_active_colors(color_t active_colors[9]);

#define PRESS_OK         0 // Action completed
#define PRESS_RESET      1 // The action caused the box to reset
#define PRESS_COMPLETED 2 // Action compleeted and the puzzle is solved

// Press a tile by its index
// Returns one the PRESS_TILE_ codes
uint8_t press_tile(uint8_t tile);

// Press an outer button by its index
// Returns one the PRESS_OUTER_ codes
uint8_t press_outer(uint8_t outer);

// Get the current state of the box
color_t get_tile_color(uint8_t tile);
color_t get_outer_color(uint8_t outer);
bool get_outer_state(uint8_t outer);
bool is_solved(void);

#endif // MORAJAI_H
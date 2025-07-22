#include "morajai.h"

// State
bool morajai_init = false;
color_t morajai_target_colors[4];
bool morajai_outer_state[4];
color_t morajai_init_tile_colors[9];
color_t morajai_tile_colors[9];

void reset(void) {
	for (int i = 0; i < 9; i++) {
        morajai_tile_colors[i] = morajai_init_tile_colors[i];
    }
	for (int i = 0; i < 4; i++) {
		morajai_outer_state[i] = false;
	}
}

void init(color_t _target_colors[4], color_t _tile_colors[9]) {
    for (int i = 0; i < 4; i++) {
        morajai_target_colors[i] = _target_colors[i];
    }
    for (int i = 0; i < 9; i++) {
		morajai_init_tile_colors[i] = _tile_colors[i];
    }
	reset();
	morajai_init = true;
}

void set_active_colors(color_t _active_colors[9]) {
    for (int i = 0; i < 9; i++) {
        morajai_tile_colors[i] = _active_colors[i];
    }
}

uint8_t get_tile_to_check(uint8_t outer) {
	uint8_t check_tile = outer;
	if (outer == 1) check_tile = 2;
	if (outer == 2) check_tile = 8;
	if (outer == 3) check_tile = 6;
	return check_tile;
}

void swap_tiles(uint8_t tile1, uint8_t tile2) {
	color_t temp = morajai_tile_colors[tile1];
	morajai_tile_colors[tile1] = morajai_tile_colors[tile2];
	morajai_tile_colors[tile2] = temp;
}

// Get the index of the tile at the given offset, or -1 if out of bounds
int8_t get_offset_tile_idx(uint8_t tile, int8_t offset_x, int8_t offset_y) {
	int8_t cur_x = tile % 3;
	int8_t cur_y = tile / 3;
	if (cur_x + offset_x < 0 || cur_x + offset_x >= 3) return -1;
	if (cur_y + offset_y < 0 || cur_y + offset_y >= 3) return -1;
	return tile + offset_x + offset_y * 3;
}
int8_t get_offset_color(uint8_t tile, int8_t offset_x, int8_t offset_y) {
	int8_t offset_tile = get_offset_tile_idx(tile, offset_x, offset_y);
	if (offset_tile == -1) return -1;
	return (int8_t)morajai_tile_colors[offset_tile];
}

// Stubs for each color
void sub_press_gray(uint8_t tile) { /* No action */}
void sub_press_red(uint8_t tile) {
	// Turn all white tiles black, and all black tiles red
	for (int i = 0; i < 9; i++) {
		if (morajai_tile_colors[i] == C_WH) {
			morajai_tile_colors[i] = C_BK;
		} else if (morajai_tile_colors[i] == C_BK) {
			morajai_tile_colors[i] = morajai_tile_colors[tile]; // C_RD, except use self to propagate blue behavior
		}
	}
}
void sub_press_green(uint8_t tile) {
	// Swap with opposing tile
	swap_tiles(tile, 8 - tile);
}
void sub_press_yellow(uint8_t tile) {
	// Tile moves north
	if (tile < 3) return;
	swap_tiles(tile, tile - 3);
}
void sub_press_purple(uint8_t tile) {
	// Tile moves south
	if (tile >= 6) return;
	swap_tiles(tile, tile + 3);
}
void sub_press_orange(uint8_t tile) {
	// Tile switches to the color of the majority of its neighbors
	uint8_t color_counts[N_COLORS+1];
	// initialize color counts
	for (int i = 0; i < N_COLORS+1; i++) {
		color_counts[i] = 0;
	}
	
	color_counts[get_offset_color(tile, -1,  0) + 1]++; // left
	color_counts[get_offset_color(tile,  1,  0) + 1]++; // right
	color_counts[get_offset_color(tile,  0, -1) + 1]++; // up
	color_counts[get_offset_color(tile,  0,  1) + 1]++; // down

	// Find the maximum
	uint8_t max_count = 0;
	uint8_t max_color = 0;
	uint8_t max_color_count = 0;
	for (int i = 0; i < N_COLORS; i++) {
		if (color_counts[i+1] > max_count) {
			max_count = color_counts[i+1];
			max_color = i;
			max_color_count = 1;
		} else if (color_counts[i+1] == max_count) {
			max_color_count++;
		}
	}

	if (max_color_count == 1) morajai_tile_colors[tile] = (color_t)(max_color);
	
}
void sub_press_pink(uint8_t tile) {
	// Rotate clockwise all adjacent (inc. diagonal) tiles, skipping anything off edge
	int8_t offset_tiles[8] = {
		get_offset_tile_idx(tile, -1,  0),
		get_offset_tile_idx(tile, -1,  1),
		get_offset_tile_idx(tile,  0,  1),
		get_offset_tile_idx(tile,  1,  1),
		get_offset_tile_idx(tile,  1,  0),
		get_offset_tile_idx(tile,  1, -1),
		get_offset_tile_idx(tile,  0, -1),
		get_offset_tile_idx(tile, -1, -1),
	};
	// Now rebuild the list, skipping -1s
	uint8_t total_tiles = 0;
	for (int i = 0; i < 8; i++) {
		if (offset_tiles[i] != -1) {
			offset_tiles[total_tiles] = offset_tiles[i];
			total_tiles++;
		}
	}
	// Now rotate the tiles
	color_t temp = morajai_tile_colors[offset_tiles[0]];
	for (int i = 0; i < total_tiles - 1; i++) {
		morajai_tile_colors[offset_tiles[i]] = morajai_tile_colors[offset_tiles[i+1]];
	}
	morajai_tile_colors[offset_tiles[total_tiles-1]] = temp;
}
void sub_press_white(uint8_t tile) {
	// Invert (grey -> white, white -> grey) adjacent grey tiles and self
	int8_t offset_tiles[4] = {
		get_offset_tile_idx(tile, -1,  0),
		get_offset_tile_idx(tile,  1,  0),
		get_offset_tile_idx(tile,  0, -1),
		get_offset_tile_idx(tile,  0,  1)
	};
	color_t self_color = morajai_tile_colors[tile]; // C_WH, except use self to propagate blue behavior
	for (int i = 0; i < 4; i++) {
		if (offset_tiles[i] != -1 && morajai_tile_colors[offset_tiles[i]] == C_GY) {
			morajai_tile_colors[offset_tiles[i]] = self_color;
		} else if (offset_tiles[i] != -1 && morajai_tile_colors[offset_tiles[i]] == self_color) {
			morajai_tile_colors[offset_tiles[i]] = C_GY;
		}
	}
	morajai_tile_colors[tile] = C_GY;
}
void sub_press_black(uint8_t tile) {
	// Rotate the row to the right
	uint8_t tile0 = tile / 3 * 3;
	uint8_t tile1 = tile0 + 1;
	uint8_t tile2 = tile0 + 2;
	color_t temp = morajai_tile_colors[tile0];
	morajai_tile_colors[tile0] = morajai_tile_colors[tile2];
	morajai_tile_colors[tile2] = morajai_tile_colors[tile1];
	morajai_tile_colors[tile1] = temp;
}
void sub_press_blue(uint8_t tile) {
	// Perform action of center tile
	switch (morajai_tile_colors[4]) {
		case C_GY: sub_press_gray(tile); break;
		case C_RD: sub_press_red(tile); break;
		case C_GN: sub_press_green(tile); break;
		case C_BK: sub_press_black(tile); break;
		case C_WH: sub_press_white(tile); break;
		case C_PI: sub_press_pink(tile); break;
		case C_PU: sub_press_purple(tile); break;
		case C_YE: sub_press_yellow(tile); break;
		case C_BU: break;
		case C_OR: sub_press_orange(tile); break;
		default: break;
	}
}


uint8_t press_tile(uint8_t tile) {
	if (tile >= 9) return PRESS_OK;
	if (is_solved()) return PRESS_COMPLETED;

	switch (morajai_tile_colors[tile]) {
		case C_GY: sub_press_gray(tile); break;
		case C_RD: sub_press_red(tile); break;
		case C_GN: sub_press_green(tile); break;
		case C_BK: sub_press_black(tile); break;
		case C_WH: sub_press_white(tile); break;
		case C_PI: sub_press_pink(tile); break;
		case C_PU: sub_press_purple(tile); break;
		case C_YE: sub_press_yellow(tile); break;
		case C_BU: sub_press_blue(tile); break;
		case C_OR: sub_press_orange(tile); break;
		default: break;
	}

	// Now check if any completed outers got messed up
	for (int i = 0; i < 4; i++) {
		uint8_t check_tile = get_tile_to_check(i);
		if (morajai_outer_state[i] // Was solved
					// And tile is now wrong
				&& morajai_tile_colors[check_tile] != morajai_target_colors[i]) {
			
			morajai_outer_state[i] = false;
		}
	}
    return PRESS_OK;
}

uint8_t press_outer(uint8_t outer) {
	if (outer >= 4) return PRESS_OK;

	uint8_t check_tile = get_tile_to_check(outer);

	if (morajai_tile_colors[check_tile] == morajai_target_colors[outer]) {
		morajai_outer_state[outer] = true;
		return is_solved() ? PRESS_COMPLETED : PRESS_OK;
	} else {
		morajai_outer_state[outer] = false;
		reset();
		return PRESS_RESET;
	}
}

color_t get_tile_color(uint8_t tile) {
	if (!morajai_init || tile >= 9) return C_GY;
	return morajai_tile_colors[tile];
}

color_t get_outer_color(uint8_t outer) {
	if (!morajai_init || outer >= 4) return C_GY;
	return morajai_outer_state[outer] ? morajai_target_colors[outer] : C_GY;
}

bool get_outer_state(uint8_t outer) {
	if (!morajai_init || outer >= 4) return false;
	return morajai_outer_state[outer];
}

bool is_solved(void) {
	if (!morajai_init) return false;
	return morajai_outer_state[0] && morajai_outer_state[1] && morajai_outer_state[2] && morajai_outer_state[3];
}

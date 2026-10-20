/**
 * College E-Library Floor Plan Configuration
 * Meticulously mapped from the hand-drawn floor plan sketch (media_1792500140221.jpg)
 * Total: 68 bookable workstations, Reception Desk, Entrance, Pillar, Door, and exact architectural partition lines.
 * ONLY boundaries present in the reference drawing are rendered (NO phantom outer walls or arbitrary enclosures).
 */

export const ROOM_DIMENSIONS = {
  width: 760,
  height: 900,
  viewBox: '0 0 760 900',
};

export const RECEPTION_DESK = {
  x: 60,
  y: 30,
  width: 150,
  height: 110,
  label: 'Reception\nDesk',
};

export const ENTRANCE = {
  x: 60,
  yStart: 175,
  yEnd: 225,
  label: 'Entrance',
};

export const CENTRAL_PILLAR = {
  x: 280,
  y: 395,
  width: 75,
  height: 90,
  label: 'Pillar',
};

export const EXIT_DOOR = {
  xStart: 370,
  xEnd: 420,
  y: 838,
  label: 'door',
};

// 68 numbered workstation layout coordinates (x, y, width=44, height=40)
export const WORKSTATION_LAYOUT = [
  // Top row area: Column next to Reception Desk: 2 (top), 1 (bottom)
  { id: 1, x: 218, y: 92, width: 44, height: 40, cluster: 'North Entrance Pod' },
  { id: 2, x: 218, y: 38, width: 44, height: 40, cluster: 'North Entrance Pod' },
  // Column 2: 4 (top), 3 (bottom)
  { id: 3, x: 286, y: 92, width: 44, height: 40, cluster: 'North Entrance Pod' },
  { id: 4, x: 286, y: 38, width: 44, height: 40, cluster: 'North Entrance Pod' },

  // Top Wall Row: 5, 6, 7, 8, 9
  { id: 5, x: 355, y: 38, width: 44, height: 40, cluster: 'North Wall Row' },
  { id: 6, x: 417, y: 38, width: 44, height: 40, cluster: 'North Wall Row' },
  { id: 7, x: 479, y: 38, width: 44, height: 40, cluster: 'North Wall Row' },
  { id: 8, x: 541, y: 38, width: 44, height: 40, cluster: 'North Wall Row' },
  { id: 9, x: 603, y: 38, width: 44, height: 40, cluster: 'North Wall Row' },

  // Upper Right Row under 7-9: 10, 11, 12, 13
  { id: 10, x: 459, y: 148, width: 44, height: 40, cluster: 'North-East Study Row' },
  { id: 11, x: 521, y: 148, width: 44, height: 40, cluster: 'North-East Study Row' },
  { id: 12, x: 583, y: 148, width: 44, height: 40, cluster: 'North-East Study Row' },
  { id: 13, x: 645, y: 148, width: 44, height: 40, cluster: 'North-East Study Row' },

  // Middle-Left Row 1: 14, 15, 16, 17, 18
  { id: 14, x: 69, y: 252, width: 44, height: 40, cluster: 'West Aisle Row 1' },
  { id: 15, x: 131, y: 252, width: 44, height: 40, cluster: 'West Aisle Row 1' },
  { id: 16, x: 193, y: 252, width: 44, height: 40, cluster: 'West Aisle Row 1' },
  { id: 17, x: 255, y: 252, width: 44, height: 40, cluster: 'West Aisle Row 1' },
  { id: 18, x: 317, y: 252, width: 44, height: 40, cluster: 'West Aisle Row 1' },

  // Middle-Right Row 1: 19, 20, 21, 22
  { id: 19, x: 459, y: 252, width: 44, height: 40, cluster: 'East Aisle Row 1' },
  { id: 20, x: 521, y: 252, width: 44, height: 40, cluster: 'East Aisle Row 1' },
  { id: 21, x: 583, y: 252, width: 44, height: 40, cluster: 'East Aisle Row 1' },
  { id: 22, x: 645, y: 252, width: 44, height: 40, cluster: 'East Aisle Row 1' },

  // Middle-Left Row 2: 23, 24, 25, 26
  { id: 23, x: 69, y: 323, width: 44, height: 40, cluster: 'West Aisle Row 2' },
  { id: 24, x: 131, y: 323, width: 44, height: 40, cluster: 'West Aisle Row 2' },
  { id: 25, x: 193, y: 323, width: 44, height: 40, cluster: 'West Aisle Row 2' },
  { id: 26, x: 255, y: 323, width: 44, height: 40, cluster: 'West Aisle Row 2' },

  // Middle-Right Study Bay: 27, 28, 29, 30
  { id: 27, x: 459, y: 323, width: 44, height: 40, cluster: 'East Study Bay' },
  { id: 28, x: 521, y: 323, width: 44, height: 40, cluster: 'East Study Bay' },
  { id: 29, x: 583, y: 323, width: 44, height: 40, cluster: 'East Study Bay' },
  { id: 30, x: 645, y: 323, width: 44, height: 40, cluster: 'East Study Bay' },

  // Center-Left Study Nook (beside Pillar): 31, 32 (top) & 35, 36 (bottom)
  { id: 31, x: 131, y: 385, width: 44, height: 40, cluster: 'Central Pillar Pod' },
  { id: 32, x: 193, y: 385, width: 44, height: 40, cluster: 'Central Pillar Pod' },

  // Middle-Right Bay Sides: 33 (left alcove), 34 (right spine)
  { id: 33, x: 459, y: 380, width: 44, height: 40, cluster: 'East Study Bay' },
  { id: 34, x: 645, y: 385, width: 44, height: 40, cluster: 'East Study Bay' },

  // Center-Left Study Nook Bottom Row: 35, 36
  { id: 35, x: 131, y: 455, width: 44, height: 40, cluster: 'Central Pillar Pod' },
  { id: 36, x: 193, y: 455, width: 44, height: 40, cluster: 'Central Pillar Pod' },

  // Middle-Right Bay Lower Side: 37
  { id: 37, x: 645, y: 455, width: 44, height: 40, cluster: 'East Study Bay' },

  // Row below Pillar/Nook: 38, 39, 40, 41
  { id: 38, x: 69, y: 517, width: 44, height: 40, cluster: 'West Lower Row 1' },
  { id: 39, x: 131, y: 517, width: 44, height: 40, cluster: 'West Lower Row 1' },
  { id: 40, x: 193, y: 517, width: 44, height: 40, cluster: 'West Lower Row 1' },
  { id: 41, x: 255, y: 517, width: 44, height: 40, cluster: 'West Lower Row 1' },

  // Row below East Study Bay: 42, 43, 44, 45
  { id: 42, x: 459, y: 517, width: 44, height: 40, cluster: 'East Lower Row 1' },
  { id: 43, x: 521, y: 517, width: 44, height: 40, cluster: 'East Lower Row 1' },
  { id: 44, x: 583, y: 517, width: 44, height: 40, cluster: 'East Lower Row 1' },
  { id: 45, x: 645, y: 517, width: 44, height: 40, cluster: 'East Lower Row 1' },

  // Middle-Left Row 5: 46, 47, 48, 49, 50
  { id: 46, x: 69, y: 603, width: 44, height: 40, cluster: 'West Lower Row 2' },
  { id: 47, x: 131, y: 603, width: 44, height: 40, cluster: 'West Lower Row 2' },
  { id: 48, x: 193, y: 603, width: 44, height: 40, cluster: 'West Lower Row 2' },
  { id: 49, x: 255, y: 603, width: 44, height: 40, cluster: 'West Lower Row 2' },
  { id: 50, x: 317, y: 603, width: 44, height: 40, cluster: 'West Lower Row 2' },

  // East Back-to-Back Pods Top Row: 51, 52, 53, 54
  { id: 51, x: 459, y: 603, width: 44, height: 40, cluster: 'South-East Pods (Upper)' },
  { id: 52, x: 521, y: 603, width: 44, height: 40, cluster: 'South-East Pods (Upper)' },
  { id: 53, x: 583, y: 603, width: 44, height: 40, cluster: 'South-East Pods (Upper)' },
  { id: 54, x: 645, y: 603, width: 44, height: 40, cluster: 'South-East Pods (Upper)' },

  // East Back-to-Back Pods Bottom Row: 55, 56, 57, 58
  { id: 55, x: 459, y: 657, width: 44, height: 40, cluster: 'South-East Pods (Lower)' },
  { id: 56, x: 521, y: 657, width: 44, height: 40, cluster: 'South-East Pods (Lower)' },
  { id: 57, x: 583, y: 657, width: 44, height: 40, cluster: 'South-East Pods (Lower)' },
  { id: 58, x: 645, y: 657, width: 44, height: 40, cluster: 'South-East Pods (Lower)' },

  // Bottom-Left Cluster (6-desk island): Top row: 59, 60, 61
  { id: 59, x: 169, y: 737, width: 44, height: 40, cluster: 'South Island Pod' },
  { id: 60, x: 231, y: 737, width: 44, height: 40, cluster: 'South Island Pod' },
  { id: 61, x: 293, y: 737, width: 44, height: 40, cluster: 'South Island Pod' },

  // Bottom-Left Cluster: Bottom row: 62, 63, 64
  { id: 62, x: 169, y: 791, width: 44, height: 40, cluster: 'South Island Pod' },
  { id: 63, x: 231, y: 791, width: 44, height: 40, cluster: 'South Island Pod' },
  { id: 64, x: 293, y: 791, width: 44, height: 40, cluster: 'South Island Pod' },

  // Bottom-Right Wall Row: 65, 66, 67, 68
  { id: 65, x: 459, y: 791, width: 44, height: 40, cluster: 'South Wall Row' },
  { id: 66, x: 521, y: 791, width: 44, height: 40, cluster: 'South Wall Row' },
  { id: 67, x: 583, y: 791, width: 44, height: 40, cluster: 'South Wall Row' },
  { id: 68, x: 645, y: 791, width: 44, height: 40, cluster: 'South Wall Row' },
];

/**
 * Architectural partition lines and desk spines strictly matching the hand-drawn sketch.
 * Only the boundaries drawn in the reference image are included.
 */
export const ARCHITECTURAL_PARTITIONS = [
  // 1. Top Section (Reception Desk + Seats 1-4 + Seats 5-9)
  // Continuous top line starting from top-left of Reception Desk, passing over 2, 4, 5, 6, 7, 8, 9
  { type: 'line', x1: 60, y1: 30, x2: 656, y2: 30, className: 'arch-spine' },
  // Reception Desk boundaries
  { type: 'line', x1: 60, y1: 30, x2: 60, y2: 140, className: 'arch-spine' }, // left line of Reception Desk
  { type: 'line', x1: 60, y1: 140, x2: 346, y2: 140, className: 'arch-spine' }, // bottom line spanning Reception Desk and Seats 1 & 3
  { type: 'line', x1: 210, y1: 30, x2: 210, y2: 140, className: 'arch-spine' }, // divider between Reception Desk and Seats 2/1
  // Dividers within Seats 1, 2, 3, 4
  { type: 'line', x1: 210, y1: 86, x2: 346, y2: 86, className: 'arch-divider' }, // horizontal line separating (2,4) and (1,3)
  { type: 'line', x1: 278, y1: 30, x2: 278, y2: 140, className: 'arch-divider' }, // vertical divider between (2,1) and (4,3)
  { type: 'line', x1: 346, y1: 30, x2: 346, y2: 140, className: 'arch-spine' }, // vertical line right of (4,3)
  // Downward ticks for Seats 5, 6, 7, 8, 9 (from top line y=30 down to y=84)
  { type: 'line', x1: 408, y1: 30, x2: 408, y2: 84, className: 'arch-tick' }, // 5|6
  { type: 'line', x1: 470, y1: 30, x2: 470, y2: 84, className: 'arch-tick' }, // 6|7
  { type: 'line', x1: 532, y1: 30, x2: 532, y2: 84, className: 'arch-tick' }, // 7|8
  { type: 'line', x1: 594, y1: 30, x2: 594, y2: 84, className: 'arch-tick' }, // 8|9
  { type: 'line', x1: 656, y1: 30, x2: 656, y2: 84, className: 'arch-tick' }, // right tick of 9

  // 2. Row 10, 11, 12, 13 (Bottom spine line with upward ticks)
  { type: 'line', x1: 450, y1: 195, x2: 698, y2: 195, className: 'arch-spine' },
  { type: 'line', x1: 450, y1: 195, x2: 450, y2: 141, className: 'arch-tick' }, // left of 10
  { type: 'line', x1: 512, y1: 195, x2: 512, y2: 141, className: 'arch-tick' }, // 10|11
  { type: 'line', x1: 574, y1: 195, x2: 574, y2: 141, className: 'arch-tick' }, // 11|12
  { type: 'line', x1: 636, y1: 195, x2: 636, y2: 141, className: 'arch-tick' }, // 12|13
  { type: 'line', x1: 698, y1: 195, x2: 698, y2: 141, className: 'arch-tick' }, // right of 13

  // 3. Row 14, 15, 16, 17, 18 (Top spine line with downward ticks)
  { type: 'line', x1: 60, y1: 245, x2: 370, y2: 245, className: 'arch-spine' },
  { type: 'line', x1: 60, y1: 245, x2: 60, y2: 299, className: 'arch-tick' }, // left of 14
  { type: 'line', x1: 122, y1: 245, x2: 122, y2: 299, className: 'arch-tick' }, // 14|15
  { type: 'line', x1: 184, y1: 245, x2: 184, y2: 299, className: 'arch-tick' }, // 15|16
  { type: 'line', x1: 246, y1: 245, x2: 246, y2: 299, className: 'arch-tick' }, // 16|17
  { type: 'line', x1: 308, y1: 245, x2: 308, y2: 299, className: 'arch-tick' }, // 17|18
  { type: 'line', x1: 370, y1: 245, x2: 370, y2: 299, className: 'arch-tick' }, // right of 18

  // 4. Row 19, 20, 21, 22 (Top spine line with downward ticks)
  { type: 'line', x1: 450, y1: 245, x2: 698, y2: 245, className: 'arch-spine' },
  { type: 'line', x1: 450, y1: 245, x2: 450, y2: 299, className: 'arch-tick' }, // left of 19
  { type: 'line', x1: 512, y1: 245, x2: 512, y2: 299, className: 'arch-tick' }, // 19|20
  { type: 'line', x1: 574, y1: 245, x2: 574, y2: 299, className: 'arch-tick' }, // 20|21
  { type: 'line', x1: 636, y1: 245, x2: 636, y2: 299, className: 'arch-tick' }, // 21|22
  { type: 'line', x1: 698, y1: 245, x2: 698, y2: 299, className: 'arch-tick' }, // right of 22

  // 5. Left Cluster: 23-26, 31-32, 35-36, 38-41
  // Bottom spine for 23-26 with upward ticks
  { type: 'line', x1: 60, y1: 370, x2: 308, y2: 370, className: 'arch-spine' },
  { type: 'line', x1: 60, y1: 370, x2: 60, y2: 316, className: 'arch-tick' }, // left of 23
  { type: 'line', x1: 122, y1: 370, x2: 122, y2: 316, className: 'arch-tick' }, // 23|24
  { type: 'line', x1: 246, y1: 370, x2: 246, y2: 316, className: 'arch-tick' }, // 25|26
  { type: 'line', x1: 308, y1: 370, x2: 308, y2: 316, className: 'arch-tick' }, // right of 26
  // Central vertical spine connecting 24|25 down to 39|40
  { type: 'line', x1: 184, y1: 316, x2: 184, y2: 564, className: 'arch-spine' },
  // Horizontal divider between (31,32) and (35,36)
  { type: 'line', x1: 122, y1: 440, x2: 246, y2: 440, className: 'arch-divider' },
  // Top spine for 38-41 with downward ticks
  { type: 'line', x1: 60, y1: 510, x2: 308, y2: 510, className: 'arch-spine' },
  { type: 'line', x1: 60, y1: 510, x2: 60, y2: 564, className: 'arch-tick' }, // left of 38
  { type: 'line', x1: 122, y1: 510, x2: 122, y2: 564, className: 'arch-tick' }, // 38|39
  { type: 'line', x1: 246, y1: 510, x2: 246, y2: 564, className: 'arch-tick' }, // 40|41
  { type: 'line', x1: 308, y1: 510, x2: 308, y2: 564, className: 'arch-tick' }, // right of 41

  // 6. Right Cluster: 27-30, 33, 34, 37, 42-45
  // Bottom spine for 27-30 with upward ticks
  { type: 'line', x1: 450, y1: 370, x2: 698, y2: 370, className: 'arch-spine' },
  { type: 'line', x1: 512, y1: 370, x2: 512, y2: 316, className: 'arch-tick' }, // 27|28
  { type: 'line', x1: 574, y1: 370, x2: 574, y2: 316, className: 'arch-tick' }, // 28|29
  { type: 'line', x1: 636, y1: 370, x2: 636, y2: 316, className: 'arch-tick' }, // 29|30
  // Left corner for 33: vertical line down on left of 33, horizontal line under 33
  { type: 'line', x1: 450, y1: 316, x2: 450, y2: 430, className: 'arch-spine' },
  { type: 'line', x1: 450, y1: 430, x2: 512, y2: 430, className: 'arch-spine' },
  // Right continuous spine passing through 30, 34, 37, 45
  { type: 'line', x1: 698, y1: 316, x2: 698, y2: 564, className: 'arch-spine' },
  // Horizontal dividers for 34 & 37
  { type: 'line', x1: 636, y1: 440, x2: 698, y2: 440, className: 'arch-divider' },
  // Top spine for 42-45 with downward ticks
  { type: 'line', x1: 450, y1: 510, x2: 698, y2: 510, className: 'arch-spine' },
  { type: 'line', x1: 450, y1: 510, x2: 450, y2: 564, className: 'arch-tick' }, // left of 42
  { type: 'line', x1: 512, y1: 510, x2: 512, y2: 564, className: 'arch-tick' }, // 42|43
  { type: 'line', x1: 574, y1: 510, x2: 574, y2: 564, className: 'arch-tick' }, // 43|44
  { type: 'line', x1: 636, y1: 510, x2: 636, y2: 564, className: 'arch-tick' }, // 44|45

  // 7. Row 46, 47, 48, 49, 50 (Bottom spine with upward ticks)
  { type: 'line', x1: 60, y1: 650, x2: 370, y2: 650, className: 'arch-spine' },
  { type: 'line', x1: 60, y1: 650, x2: 60, y2: 596, className: 'arch-tick' }, // left of 46
  { type: 'line', x1: 122, y1: 650, x2: 122, y2: 596, className: 'arch-tick' }, // 46|47
  { type: 'line', x1: 184, y1: 650, x2: 184, y2: 596, className: 'arch-tick' }, // 47|48
  { type: 'line', x1: 246, y1: 650, x2: 246, y2: 596, className: 'arch-tick' }, // 48|49
  { type: 'line', x1: 308, y1: 650, x2: 308, y2: 596, className: 'arch-tick' }, // 49|50
  { type: 'line', x1: 370, y1: 650, x2: 370, y2: 596, className: 'arch-tick' }, // right of 50

  // 8. Double Row 51-54 and 55-58 (Central horizontal spine with crossing vertical dividers)
  { type: 'line', x1: 450, y1: 650, x2: 698, y2: 650, className: 'arch-spine' },
  { type: 'line', x1: 450, y1: 596, x2: 450, y2: 704, className: 'arch-divider' }, // left of 51 & 55
  { type: 'line', x1: 512, y1: 596, x2: 512, y2: 704, className: 'arch-divider' }, // 51|52 & 55|56
  { type: 'line', x1: 574, y1: 596, x2: 574, y2: 704, className: 'arch-divider' }, // 52|53 & 56|57
  { type: 'line', x1: 636, y1: 596, x2: 636, y2: 704, className: 'arch-divider' }, // 53|54 & 57|58
  { type: 'line', x1: 698, y1: 596, x2: 698, y2: 704, className: 'arch-divider' }, // right of 54 & 58

  // 9. Island Pod 59-64 (Enclosed 2x3 pod with internal cross lines)
  { type: 'line', x1: 160, y1: 730, x2: 346, y2: 730, className: 'arch-spine' }, // top line
  { type: 'line', x1: 160, y1: 784, x2: 346, y2: 784, className: 'arch-divider' }, // central horizontal divider
  { type: 'line', x1: 160, y1: 838, x2: 370, y2: 838, className: 'arch-spine' }, // bottom line under 62,63,64 extending to door at x=370
  { type: 'line', x1: 160, y1: 730, x2: 160, y2: 838, className: 'arch-spine' }, // left line
  { type: 'line', x1: 222, y1: 730, x2: 222, y2: 838, className: 'arch-divider' }, // vertical divider between 59|60 & 62|63
  { type: 'line', x1: 284, y1: 730, x2: 284, y2: 838, className: 'arch-divider' }, // vertical divider between 60|61 & 63|64
  { type: 'line', x1: 346, y1: 730, x2: 346, y2: 838, className: 'arch-spine' }, // right line of 61 & 64

  // 10. Connection from Door to Row 65-68
  { type: 'line', x1: 420, y1: 838, x2: 450, y2: 838, className: 'arch-spine' },

  // 11. Row 65, 66, 67, 68 (Bottom spine with upward ticks)
  { type: 'line', x1: 450, y1: 838, x2: 698, y2: 838, className: 'arch-spine' },
  { type: 'line', x1: 450, y1: 838, x2: 450, y2: 784, className: 'arch-tick' }, // left of 65
  { type: 'line', x1: 512, y1: 838, x2: 512, y2: 784, className: 'arch-tick' }, // 65|66
  { type: 'line', x1: 574, y1: 838, x2: 574, y2: 784, className: 'arch-tick' }, // 66|67
  { type: 'line', x1: 636, y1: 838, x2: 636, y2: 784, className: 'arch-tick' }, // 67|68
  { type: 'line', x1: 698, y1: 838, x2: 698, y2: 784, className: 'arch-tick' }, // right of 68
];

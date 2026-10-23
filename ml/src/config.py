"""
Configuration constants for QueueSense Crowd Analytics ML Pipeline.
"""

from pathlib import Path

# Paths
BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
SYNTHETIC_DATA_DIR = DATA_DIR / "synthetic"
REAL_DATA_DIR = DATA_DIR / "real"
TEMPLATES_DIR = DATA_DIR / "templates"
MODELS_DIR = BASE_DIR / "models"

# Ensure directories exist
for directory in [DATA_DIR, SYNTHETIC_DATA_DIR, REAL_DATA_DIR, TEMPLATES_DIR, MODELS_DIR]:
    directory.mkdir(parents=True, exist_ok=True)

# Horizons for crowd prediction (in minutes)
FORECAST_HORIZONS = [15, 30, 60]

# Features strictly available prior to forecast time (zero target leakage)
FEATURE_COLUMNS = [
    "hour",
    "minute",
    "day_of_week",
    "is_weekend",
    "occupancy_now",
    "occupancy_lag15",
    "occupancy_lag30",
    "occupancy_lag60",
    "rolling_mean_1h",
    "delta_15m"
]

RANDOM_STATE = 42

# Operating hours (Library: 08:00 to 22:00)
OPERATING_HOURS_START = 8
OPERATING_HOURS_END = 22

# Crowd density thresholds (as % of capacity)
THRESHOLD_LOW = 0.40       # < 40%
THRESHOLD_MODERATE = 0.75  # 40% to 75%
THRESHOLD_HIGH = 0.90      # 75% to 90%
# >= 90% is FULL

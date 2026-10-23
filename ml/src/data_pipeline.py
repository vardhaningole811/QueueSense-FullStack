"""
Data Ingestion, Validation, and Feature Engineering Pipeline.

Provides clean separation between raw observations and engineered features.
Strictly ensures zero target leakage for time-series forecasting.
"""

from typing import Tuple, List, Optional
import pandas as pd
import numpy as np
from config import FEATURE_COLUMNS, FORECAST_HORIZONS


def validate_raw_observations(df: pd.DataFrame) -> Tuple[pd.DataFrame, List[str]]:
    """
    Validates a raw DataFrame of observations (either synthetic or real CSV).
    Checks:
    - Required columns present
    - Valid timestamps that can be parsed
    - Chronological sorting
    - Duplicate timestamp detection
    - Non-negative, sensible headcount estimates
    """
    issues = []
    
    required = ["observation_timestamp", "people_present_estimate"]
    for col in required:
        if col not in df.columns:
            raise ValueError(f"Missing required column in observation dataset: '{col}'")
            
    clean_df = df.copy()
    
    # 1. Parse timestamps
    clean_df["observation_timestamp"] = pd.to_datetime(
        clean_df["observation_timestamp"],
        errors="coerce"
    )
    invalid_dates = clean_df["observation_timestamp"].isna().sum()
    if invalid_dates > 0:
        issues.append(f"Dropped {invalid_dates} rows with invalid timestamp format.")
        clean_df = clean_df.dropna(subset=["observation_timestamp"])
        
    # 2. Sort chronologically
    clean_df = clean_df.sort_values(by="observation_timestamp").reset_index(drop=True)
    
    # 3. Check and drop duplicate timestamps
    dups = clean_df.duplicated(subset=["observation_timestamp"]).sum()
    if dups > 0:
        issues.append(f"Removed {dups} duplicate timestamp records, keeping latest.")
        clean_df = clean_df.drop_duplicates(subset=["observation_timestamp"], keep="last").reset_index(drop=True)
        
    # 4. Clean people_present_estimate
    clean_df["people_present_estimate"] = pd.to_numeric(
        clean_df["people_present_estimate"],
        errors="coerce"
    )
    invalid_counts = clean_df["people_present_estimate"].isna().sum()
    if invalid_counts > 0:
        issues.append(f"Dropped {invalid_counts} rows with non-numeric headcount estimates.")
        clean_df = clean_df.dropna(subset=["people_present_estimate"])
        
    # Check boundaries (e.g. negative or absurdly large values > 1000)
    neg_mask = clean_df["people_present_estimate"] < 0
    if neg_mask.sum() > 0:
        issues.append(f"Clipped {neg_mask.sum()} negative headcount entries to 0.")
        clean_df.loc[neg_mask, "people_present_estimate"] = 0
        
    absurd_mask = clean_df["people_present_estimate"] > 1000
    if absurd_mask.sum() > 0:
        issues.append(f"Filtered {absurd_mask.sum()} unrealistic headcount entries (> 1000).")
        clean_df = clean_df[~absurd_mask]
        
    return clean_df.reset_index(drop=True), issues


def build_feature_dataset(
    clean_df: pd.DataFrame,
    is_training: bool = True
) -> pd.DataFrame:
    """
    Transforms clean observations into feature vectors with strictly past-only lag features.
    
    Time step assumption: 15-minute intervals.
    Lags:
    - lag15: t-1 (15 min prior)
    - lag30: t-2 (30 min prior)
    - lag60: t-4 (60 min prior)
    
    Targets (if is_training):
    - target_t15: t+1 (15 min ahead)
    - target_t30: t+2 (30 min ahead)
    - target_t60: t+4 (60 min ahead)
    """
    df = clean_df.copy()
    
    # Calendar features
    ts = df["observation_timestamp"]
    df["hour"] = ts.dt.hour
    df["minute"] = ts.dt.minute
    df["day_of_week"] = ts.dt.day_of_week
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)
    
    # Core occupancy
    df["occupancy_now"] = df["people_present_estimate"].astype(float)
    
    # Strictly past-only lag features
    df["occupancy_lag15"] = df["occupancy_now"].shift(1)
    df["occupancy_lag30"] = df["occupancy_now"].shift(2)
    df["occupancy_lag60"] = df["occupancy_now"].shift(4)
    
    # Rolling 1-hour average (4 steps of 15 min), min_periods=1
    df["rolling_mean_1h"] = df["occupancy_now"].rolling(window=4, min_periods=1).mean()
    
    # Delta (rate of change over past 15 minutes)
    df["delta_15m"] = df["occupancy_now"] - df["occupancy_lag15"]
    
    # Backward fill boundary lag NaNs at the very start of the series with current occupancy
    df["occupancy_lag15"] = df["occupancy_lag15"].bfill().fillna(df["occupancy_now"])
    df["occupancy_lag30"] = df["occupancy_lag30"].bfill().fillna(df["occupancy_now"])
    df["occupancy_lag60"] = df["occupancy_lag60"].bfill().fillna(df["occupancy_now"])
    df["delta_15m"] = df["delta_15m"].fillna(0.0)
    
    if is_training:
        # Step-ahead targets
        df["target_t15"] = df["occupancy_now"].shift(-1)
        df["target_t30"] = df["occupancy_now"].shift(-2)
        df["target_t60"] = df["occupancy_now"].shift(-4)
        
        # Drop the last 4 rows that lack future targets for evaluation
        df = df.dropna(subset=["target_t15", "target_t30", "target_t60"]).reset_index(drop=True)
        
    return df

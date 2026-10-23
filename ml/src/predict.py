"""
Inference Module for QueueSense Crowd Analytics.

Loads the best trained regression models and generates:
- Future occupancy predictions for +15, +30, and +60 minutes
- Estimated time until full capacity
- Best time to visit recommendation
- Model transparency metadata
"""

import json
from datetime import datetime, timedelta
from typing import Dict, Any, Optional, Tuple
import joblib
import numpy as np
import pandas as pd

from config import MODELS_DIR, FEATURE_COLUMNS, THRESHOLD_LOW, THRESHOLD_MODERATE, THRESHOLD_HIGH


class CrowdPredictor:

    def __init__(self):
        self.model_bundle_path = MODELS_DIR / "elibrary_forecast_models.joblib"
        self.metadata_path = MODELS_DIR / "model_metadata.json"
        self.bundle = None
        self.metadata = None
        self._load()

    def _load(self):
        if self.model_bundle_path.exists():
            try:
                self.bundle = joblib.load(self.model_bundle_path)
            except Exception as e:
                print(f"[QueueSense ML] Error loading model bundle: {e}")
                self.bundle = None

        if self.metadata_path.exists():
            try:
                with open(self.metadata_path, "r") as f:
                    self.metadata = json.load(f)
            except Exception as e:
                print(f"[QueueSense ML] Error loading metadata: {e}")
                self.metadata = None

    def is_ready(self) -> bool:
        return self.bundle is not None and self.metadata is not None

    def predict(
        self,
        current_recorded_sessions: float,
        usable_capacity: Optional[int] = 8,
        timestamp_str: Optional[str] = None,
        facility_name: str = "E-Library"
    ) -> Dict[str, Any]:
        """
        Generates multi-horizon forecasts, crowd levels, time until full, and best visit windows.
        """
        if not self.is_ready():
            self._load()
            if not self.is_ready():
                return {
                    "status": "INSUFFICIENT_DATA",
                    "message": "Model artifacts not yet generated. Please run train.py first.",
                    "forecasts": [],
                    "estimated_time_until_full_minutes": None,
                    "time_until_full_message": "Not enough data to estimate",
                    "best_time_to_visit": "Insufficient trained models to generate recommendation."
                }

        # Parse reference timestamp
        if timestamp_str:
            try:
                ref_time = datetime.fromisoformat(timestamp_str)
            except Exception:
                ref_time = datetime.now()
        else:
            ref_time = datetime.now()

        # Build feature row for current time
        hour = ref_time.hour
        minute = ref_time.minute
        day_of_week = ref_time.weekday()
        is_weekend = 1 if day_of_week >= 5 else 0
        occupancy_now = float(current_recorded_sessions)

        # Build feature vector
        # Using current occupancy as baseline for lags if single point inference
        features = pd.DataFrame([{
            "hour": hour,
            "minute": minute,
            "day_of_week": day_of_week,
            "is_weekend": is_weekend,
            "occupancy_now": occupancy_now,
            "occupancy_lag15": occupancy_now,
            "occupancy_lag30": occupancy_now,
            "occupancy_lag60": occupancy_now,
            "rolling_mean_1h": occupancy_now,
            "delta_15m": 0.0
        }])[FEATURE_COLUMNS]

        models_by_horizon = self.bundle["models_by_horizon"]
        forecasts = []
        raw_predictions = {}

        for h in [15, 30, 60]:
            target_time = ref_time + timedelta(minutes=h)
            model = models_by_horizon.get(h)
            
            if model is not None:
                raw_pred = float(model.predict(features)[0])
                # Bounded by usable capacity and 0
                max_cap = usable_capacity if usable_capacity and usable_capacity > 0 else 50
                pred = max(0.0, min(float(max_cap), raw_pred))
            else:
                pred = occupancy_now

            raw_predictions[h] = pred
            
            pred_pct = (pred / usable_capacity * 100.0) if usable_capacity and usable_capacity > 0 else None
            
            if pred_pct is None:
                crowd_level = "UNKNOWN"
            elif pred_pct < 40.0:
                crowd_level = "LOW"
            elif pred_pct < 75.0:
                crowd_level = "MODERATE"
            elif pred_pct < 90.0:
                crowd_level = "HIGH"
            else:
                crowd_level = "FULL"

            forecasts.append({
                "horizon_minutes": h,
                "target_timestamp": target_time.strftime("%I:%M %p"),
                "predicted_occupancy": round(pred, 1),
                "predicted_occupancy_percentage": round(pred_pct, 1) if pred_pct is not None else None,
                "predicted_crowd_level": crowd_level,
                "confidence_note": f"Estimated via {self.bundle['model_name']} regression model."
            })

        # Calculate estimated time until full
        time_until_full, time_until_full_msg = self._calculate_time_until_full(
            occupancy_now,
            usable_capacity,
            raw_predictions
        )

        # Calculate best time to visit recommendation
        best_visit_msg = self._recommend_best_time_to_visit(ref_time, usable_capacity)

        # Transparency info
        best_metrics = self.metadata.get("best_metrics", {})
        baseline_metrics = self.metadata.get("baseline_metrics", {})

        return {
            "status": "AVAILABLE",
            "forecasts": forecasts,
            "estimated_time_until_full_minutes": time_until_full,
            "time_until_full_message": time_until_full_msg,
            "best_time_to_visit": best_visit_msg,
            "transparency": {
                "is_prototype": self.metadata.get("is_prototype", True),
                "data_source": self.metadata.get("data_source", "SYNTHETIC PROTOTYPE DATASET"),
                "model_selected": self.metadata.get("model_selected", self.bundle["model_name"]),
                "validation_mae": best_metrics.get("val_mae_avg"),
                "validation_rmse": best_metrics.get("val_rmse_avg"),
                "baseline_mae": baseline_metrics.get("val_mae_avg"),
                "baseline_rmse": baseline_metrics.get("val_rmse_avg"),
                "horizons": [15, 30, 60],
                "disclaimer": self.metadata.get("disclaimer"),
                "training_timestamp": self.metadata.get("training_timestamp"),
                "sample_size": self.metadata.get("sample_size")
            }
        }

    def _calculate_time_until_full(
        self,
        current_occ: float,
        usable_capacity: Optional[int],
        predictions: Dict[int, float]
    ) -> Tuple[Optional[int], str]:
        if not usable_capacity or usable_capacity <= 0:
            return None, "Not enough data to estimate (capacity unverified)"

        if current_occ >= usable_capacity:
            return 0, "Facility is currently at full capacity"

        # Rate of change over 60 min horizon
        pred_60 = predictions.get(60, current_occ)
        net_change = pred_60 - current_occ
        
        # If rate is positive and substantial
        if net_change > 0.5:
            rate_per_min = net_change / 60.0
            seats_needed = usable_capacity - current_occ
            mins = int(round(seats_needed / rate_per_min))
            if 0 < mins <= 120:
                return mins, f"Estimated ~{mins} mins at current arrival pace until full capacity"

        return None, "Not enough data to estimate (occupancy is currently steady or declining)"

    def _recommend_best_time_to_visit(
        self,
        ref_time: datetime,
        usable_capacity: Optional[int]
    ) -> str:
        """
        Finds the lowest predicted density window during upcoming operating hours.
        """
        models_by_horizon = self.bundle.get("models_by_horizon", {})
        h60_model = models_by_horizon.get(60)
        
        if not h60_model:
            return "Standard library operating hours (08:00 AM - 10:00 PM)"

        candidate_hours = [9, 11, 14, 16, 19, 20]
        # Filter for future hours today or tomorrow morning
        best_hour = None
        lowest_occ = float("inf")

        for hr in candidate_hours:
            target_dt = ref_time.replace(hour=hr, minute=0, second=0)
            if target_dt < ref_time:
                # evaluate for tomorrow
                target_dt += timedelta(days=1)
                
            dow = target_dt.weekday()
            is_wknd = 1 if dow >= 5 else 0
            
            sample_df = pd.DataFrame([{
                "hour": hr,
                "minute": 0,
                "day_of_week": dow,
                "is_weekend": is_wknd,
                "occupancy_now": 2.0, # assumed reasonable estimate
                "occupancy_lag15": 2.0,
                "occupancy_lag30": 2.0,
                "occupancy_lag60": 2.0,
                "rolling_mean_1h": 2.0,
                "delta_15m": 0.0
            }])[FEATURE_COLUMNS]
            
            pred = float(h60_model.predict(sample_df)[0])
            if pred < lowest_occ:
                lowest_occ = pred
                best_hour = hr

        if best_hour is not None:
            start_str = datetime.strptime(str(best_hour), "%H").strftime("%I:00 %p")
            end_str = datetime.strptime(str((best_hour + 2) % 24), "%H").strftime("%I:00 %p")
            occ_label = f"~{max(1, int(round(lowest_occ)))} people"
            return f"Recommended window: {start_str} - {end_str} (Predicted Low crowd: {occ_label})"

        return "Morning hours (08:30 AM - 10:30 AM) generally exhibit lower crowd volume."

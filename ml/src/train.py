"""
Model Training and Evaluation Pipeline.

Compares:
1. Baseline: Naive Persistence (last observed occupancy)
2. Linear Regression
3. Decision Tree Regressor
4. Random Forest Regressor
5. Gradient Boosting Regressor

Uses strict chronological train/validation/test splitting.
Evaluates using MAE and RMSE across +15, +30, and +60 minute forecast horizons.
Selects the best performing model based on validation error and saves artifacts to ml/models/.
"""

import json
import math
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Tuple, Optional
import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.tree import DecisionTreeRegressor
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, root_mean_squared_error

from config import (
    FEATURE_COLUMNS,
    FORECAST_HORIZONS,
    RANDOM_STATE,
    MODELS_DIR,
    SYNTHETIC_DATA_DIR,
    REAL_DATA_DIR
)
from data_pipeline import validate_raw_observations, build_feature_dataset
from synthetic_generator import generate_synthetic_occupancy


def load_dataset(data_path: Optional[Path] = None) -> Tuple[pd.DataFrame, str]:
    """
    Loads dataset:
    - If real data exists in ml/data/real/*.csv, loads it and validates it.
    - Otherwise, generates and loads synthetic prototype data.
    Returns (cleaned_df, dataset_type).
    """
    if data_path and data_path.exists():
        df = pd.read_csv(data_path)
        is_synthetic = "is_synthetic" in df.columns and (bool(df["is_synthetic"].iloc[0]) or str(df["is_synthetic"].iloc[0]).lower() == "true")
        dataset_type = "SYNTHETIC_PROTOTYPE" if is_synthetic else "REAL_OBSERVATIONS"
        return df, dataset_type
        
    # Check for real data
    real_files = list(REAL_DATA_DIR.glob("*.csv"))
    if real_files:
        chosen = real_files[0]
        print(f"[QueueSense ML] Found genuine observation dataset: {chosen}")
        df = pd.read_csv(chosen)
        return df, "REAL_OBSERVATIONS"
        
    # Check for existing synthetic data
    synth_file = SYNTHETIC_DATA_DIR / "synthetic_elibrary_observations.csv"
    if not synth_file.exists():
        print("[QueueSense ML] No pre-existing dataset found. Generating synthetic prototype...")
        df = generate_synthetic_occupancy()
    else:
        df = pd.read_csv(synth_file)
        
    return df, "SYNTHETIC_PROTOTYPE"


def train_and_evaluate(data_path: Optional[Path] = None) -> Dict[str, Any]:
    raw_df, dataset_type = load_dataset(data_path)
    clean_df, issues = validate_raw_observations(raw_df)
    
    if len(clean_df) < 50:
        raise ValueError(f"Dataset too small ({len(clean_df)} records) to train reliable models. Minimum 50 required.")
        
    features_df = build_feature_dataset(clean_df, is_training=True)
    
    # Chronological Split (70% Train, 15% Validation, 15% Test)
    n = len(features_df)
    train_end = int(0.70 * n)
    val_end = int(0.85 * n)
    
    train_df = features_df.iloc[:train_end]
    val_df = features_df.iloc[train_end:val_end]
    test_df = features_df.iloc[val_end:]
    
    X_train = train_df[FEATURE_COLUMNS]
    X_val = val_df[FEATURE_COLUMNS]
    X_test = test_df[FEATURE_COLUMNS]
    
    horizons = [15, 30, 60]
    target_names = [f"target_t{h}" for h in horizons]
    
    # 1. Baseline Model (Naive Persistence: pred = occupancy_now)
    baseline_val_mae = {}
    baseline_val_rmse = {}
    baseline_test_mae = {}
    baseline_test_rmse = {}
    
    for h in horizons:
        t_col = f"target_t{h}"
        pred_val = val_df["occupancy_now"]
        pred_test = test_df["occupancy_now"]
        
        baseline_val_mae[h] = float(mean_absolute_error(val_df[t_col], pred_val))
        baseline_val_rmse[h] = float(root_mean_squared_error(val_df[t_col], pred_val))
        baseline_test_mae[h] = float(mean_absolute_error(test_df[t_col], pred_test))
        baseline_test_rmse[h] = float(root_mean_squared_error(test_df[t_col], pred_test))
        
    baseline_summary = {
        "name": "Naive Baseline (Last Observed Persistence)",
        "val_mae_avg": round(float(np.mean(list(baseline_val_mae.values()))), 3),
        "val_rmse_avg": round(float(np.mean(list(baseline_val_rmse.values()))), 3),
        "test_mae_avg": round(float(np.mean(list(baseline_test_mae.values()))), 3),
        "test_rmse_avg": round(float(np.mean(list(baseline_test_rmse.values()))), 3),
        "by_horizon": {
            f"+{h}m": {
                "val_mae": round(baseline_val_mae[h], 3),
                "val_rmse": round(baseline_val_rmse[h], 3),
                "test_mae": round(baseline_test_mae[h], 3),
                "test_rmse": round(baseline_test_rmse[h], 3)
            } for h in horizons
        }
    }
    
    # Model candidate factories
    model_factories = {
        "Linear Regression": lambda: LinearRegression(),
        "Decision Tree": lambda: DecisionTreeRegressor(max_depth=5, min_samples_split=10, random_state=RANDOM_STATE),
        "Random Forest": lambda: RandomForestRegressor(n_estimators=50, max_depth=6, random_state=RANDOM_STATE),
        "Gradient Boosting": lambda: GradientBoostingRegressor(n_estimators=50, max_depth=4, learning_rate=0.08, random_state=RANDOM_STATE)
    }
    
    model_results = {}
    fitted_models = {}
    
    print("\n" + "=" * 70)
    print(f" QUEUESENSE ML TRAINING & COMPARISON ({dataset_type})")
    print("=" * 70)
    print(f"Total clean samples: {n} (Train: {len(train_df)}, Val: {len(val_df)}, Test: {len(test_df)})")
    print(f"Features: {FEATURE_COLUMNS}")
    print(f"Horizons: {[f'+{h}m' for h in horizons]}\n")
    print(f"[Baseline] Naive Persistence Validation MAE: {baseline_summary['val_mae_avg']:.3f}, RMSE: {baseline_summary['val_rmse_avg']:.3f}")
    print("-" * 70)
    
    best_model_name = None
    best_val_mae = float("inf")
    
    for model_name, factory in model_factories.items():
        horizon_models = {}
        val_mae_list = []
        val_rmse_list = []
        test_mae_list = []
        test_rmse_list = []
        horizon_detail = {}
        
        for h in horizons:
            t_col = f"target_t{h}"
            y_train = train_df[t_col]
            y_val = val_df[t_col]
            y_test = test_df[t_col]
            
            m = factory()
            m.fit(X_train, y_train)
            horizon_models[h] = m
            
            p_val = m.predict(X_val)
            p_test = m.predict(X_test)
            
            v_mae = float(mean_absolute_error(y_val, p_val))
            v_rmse = float(root_mean_squared_error(y_val, p_val))
            t_mae = float(mean_absolute_error(y_test, p_test))
            t_rmse = float(root_mean_squared_error(y_test, p_test))
            
            val_mae_list.append(v_mae)
            val_rmse_list.append(v_rmse)
            test_mae_list.append(t_mae)
            test_rmse_list.append(t_rmse)
            
            horizon_detail[f"+{h}m"] = {
                "val_mae": round(v_mae, 3),
                "val_rmse": round(v_rmse, 3),
                "test_mae": round(t_mae, 3),
                "test_rmse": round(t_rmse, 3)
            }
            
        avg_val_mae = float(np.mean(val_mae_list))
        avg_val_rmse = float(np.mean(val_rmse_list))
        avg_test_mae = float(np.mean(test_mae_list))
        avg_test_rmse = float(np.mean(test_rmse_list))
        
        fitted_models[model_name] = horizon_models
        model_results[model_name] = {
            "name": model_name,
            "val_mae_avg": round(avg_val_mae, 3),
            "val_rmse_avg": round(avg_val_rmse, 3),
            "test_mae_avg": round(avg_test_mae, 3),
            "test_rmse_avg": round(avg_test_rmse, 3),
            "by_horizon": horizon_detail
        }
        
        print(f"[{model_name:<18}] Val MAE: {avg_val_mae:.3f} | Val RMSE: {avg_val_rmse:.3f} | Test MAE: {avg_test_mae:.3f} | Test RMSE: {avg_test_rmse:.3f}")
        
        # Select best model strictly on validation MAE
        if avg_val_mae < best_val_mae:
            best_val_mae = avg_val_mae
            best_model_name = model_name
            
    print("-" * 70)
    print(f"Best Performing Model Selected: {best_model_name} (Validation MAE: {best_val_mae:.3f})")
    print("=" * 70 + "\n")
    
    # Save the selected model artifact bundle
    best_bundle = {
        "model_name": best_model_name,
        "models_by_horizon": fitted_models[best_model_name],
        "feature_columns": FEATURE_COLUMNS,
        "horizons": horizons,
        "dataset_type": dataset_type,
        "trained_at": datetime.now().isoformat(),
        "random_state": RANDOM_STATE
    }
    
    model_artifact_path = MODELS_DIR / "elibrary_forecast_models.joblib"
    joblib.dump(best_bundle, model_artifact_path)
    
    # Save complete metadata and audit json
    metadata = {
        "model_selected": best_model_name,
        "is_prototype": (dataset_type == "SYNTHETIC_PROTOTYPE"),
        "data_source": (
            "SYNTHETIC PROTOTYPE DATASET — Real-world physical headcount validation pending"
            if dataset_type == "SYNTHETIC_PROTOTYPE"
            else "MANUAL PHYSICAL HEADCOUNT OBSERVATIONS"
        ),
        "validation_status": (
            "PROTOTYPE — REAL-WORLD VALIDATION PENDING"
            if dataset_type == "SYNTHETIC_PROTOTYPE"
            else "VALIDATED WITH REAL OBSERVATIONS"
        ),
        "disclaimer": (
            "PROTOTYPE — REAL-WORLD VALIDATION PENDING. Trained on simulated campus library patterns. Real-world model accuracy has NOT been established."
            if dataset_type == "SYNTHETIC_PROTOTYPE"
            else "Real physical observation dataset used. Continued validation recommended across varied exam and holiday periods."
        ),
        "training_timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "sample_size": n,
        "train_samples": len(train_df),
        "validation_samples": len(val_df),
        "test_samples": len(test_df),
        "horizons_minutes": horizons,
        "feature_columns": FEATURE_COLUMNS,
        "best_metrics": model_results[best_model_name],
        "baseline_metrics": baseline_summary,
        "all_models_comparison": model_results,
        "data_validation_issues": issues
    }
    
    metadata_path = MODELS_DIR / "model_metadata.json"
    with open(metadata_path, "w") as f:
        json.dump(metadata, f, indent=2)
        
    print(f"[QueueSense ML] Saved model bundle to: {model_artifact_path}")
    print(f"[QueueSense ML] Saved metadata summary to: {metadata_path}")
    
    return metadata


if __name__ == "__main__":
    train_and_evaluate()

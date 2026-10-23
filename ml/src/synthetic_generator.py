"""
Synthetic Physical Occupancy Dataset Generator for QueueSense ML Prototype.

Generates realistic time-series occupancy observations for the campus E-Library
across multiple weeks. All generated data is stored strictly in the ml/ directory
and is never written to PostgreSQL operational tables.
"""

import math
import random
from datetime import datetime, timedelta
import pandas as pd
from config import SYNTHETIC_DATA_DIR, RANDOM_STATE, OPERATING_HOURS_START, OPERATING_HOURS_END


def generate_synthetic_occupancy(
    start_date_str: str = "2026-09-01",
    days: int = 28,
    facility_name: str = "E-Library",
    usable_capacity: int = 8,
    output_filename: str = "synthetic_elibrary_observations.csv"
) -> pd.DataFrame:
    """
    Generates plausible 15-minute interval occupancy records.
    
    Occupancy curve dynamics:
    - 08:00 - 10:00: Early morning ramp-up (10-30% capacity)
    - 10:00 - 12:30: Mid-morning study rush (40-75% capacity)
    - 12:30 - 14:00: Lunch break slight dip (30-50% capacity)
    - 14:00 - 18:00: Peak afternoon study sessions (65-95% capacity)
    - 18:00 - 21:00: Evening revision (40-70% capacity)
    - 21:00 - 22:00: Closing wind-down (10-25% capacity)
    - Weekends: Moderate shift towards afternoon peak with lower early-morning attendance.
    """
    random.seed(RANDOM_STATE)
    
    start_date = datetime.strptime(start_date_str, "%Y-%m-%d")
    records = []
    
    current_occupancy = 0
    
    for day_idx in range(days):
        current_day = start_date + timedelta(days=day_idx)
        day_of_week = current_day.weekday() # 0 = Monday, 6 = Sunday
        is_weekend = 1 if day_of_week >= 5 else 0
        
        # Reset at opening
        current_occupancy = 0
        
        # Interval: 15 minutes between 08:00 and 22:00
        step_minutes = 15
        total_steps = int(((OPERATING_HOURS_END - OPERATING_HOURS_START) * 60) / step_minutes)
        
        for step in range(total_steps):
            timestamp = current_day.replace(
                hour=OPERATING_HOURS_START,
                minute=0,
                second=0
            ) + timedelta(minutes=step * step_minutes)
            
            hour_float = timestamp.hour + timestamp.minute / 60.0
            
            # Base target occupancy curve according to time of day
            if 8.0 <= hour_float < 10.0:
                base_ratio = 0.20 + 0.15 * ((hour_float - 8.0) / 2.0)
            elif 10.0 <= hour_float < 12.5:
                base_ratio = 0.45 + 0.25 * ((hour_float - 10.0) / 2.5)
            elif 12.5 <= hour_float < 14.0:
                base_ratio = 0.40 - 0.10 * math.sin((hour_float - 12.5) / 1.5 * math.pi)
            elif 14.0 <= hour_float < 18.0:
                base_ratio = 0.70 + 0.20 * math.sin((hour_float - 14.0) / 4.0 * math.pi)
            elif 18.0 <= hour_float < 21.0:
                base_ratio = 0.55 - 0.20 * ((hour_float - 18.0) / 3.0)
            else: # 21.0 to 22.0
                base_ratio = 0.20 - 0.15 * ((hour_float - 21.0) / 1.0)
                
            # Weekend adjustment (quieter mornings, steady afternoons)
            if is_weekend:
                base_ratio = base_ratio * 0.80
                
            # Day of week variation (mid-week peak on Wed/Thu)
            if day_of_week in [2, 3]: # Wed, Thu
                base_ratio = min(1.0, base_ratio * 1.10)
            elif day_of_week == 6: # Sunday
                base_ratio = base_ratio * 0.75
                
            target_headcount = base_ratio * usable_capacity
            
            # Simulated smooth movement towards target with noise
            diff = target_headcount - current_occupancy
            delta = int(round(diff * 0.40 + random.uniform(-1.2, 1.2)))
            
            new_occupancy = max(0, min(usable_capacity, current_occupancy + delta))
            
            arrived = max(0, new_occupancy - current_occupancy + random.randint(0, 1))
            left = max(0, arrived - (new_occupancy - current_occupancy))
            
            current_occupancy = new_occupancy
            
            records.append({
                "observation_timestamp": timestamp.strftime("%Y-%m-%d %H:%M:%S"),
                "facility_name": facility_name,
                "people_present_estimate": current_occupancy,
                "usable_capacity": usable_capacity,
                "people_arrived_since_previous": arrived,
                "people_left_since_previous": left,
                "is_synthetic": True,
                "notes": "SYNTHETIC_PROTOTYPE — Simulated campus library observation"
            })
            
    df = pd.DataFrame(records)
    output_path = SYNTHETIC_DATA_DIR / output_filename
    df.to_csv(output_path, index=False)
    print(f"[QueueSense ML] Generated {len(df)} synthetic observations saved to: {output_path}")
    return df


if __name__ == "__main__":
    generate_synthetic_occupancy()

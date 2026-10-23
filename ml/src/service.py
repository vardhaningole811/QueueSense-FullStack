"""
Flask REST Microservice for QueueSense Crowd Analytics Inference.

Exposes internal endpoints for Spring Boot:
- GET  /health   -> Service health check
- POST /predict  -> Generate forecasts (+15, +30, +60), crowd level, time until full, best time to visit
- GET  /metadata -> Model comparison table, validation scores, and transparency info
"""

import os
from datetime import datetime
from flask import Flask, request, jsonify
from predict import CrowdPredictor

app = Flask(__name__)
predictor = CrowdPredictor()


@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "UP",
        "service": "QueueSense ML Crowd Analytics",
        "model_ready": predictor.is_ready(),
        "timestamp": datetime.now().isoformat()
    }), 200


@app.route("/predict", methods=["POST"])
def predict():
    try:
        data = request.get_json(force=True, silent=True) or {}
        
        current_sessions = float(data.get("current_recorded_sessions", 0))
        usable_capacity = data.get("usable_capacity")
        if usable_capacity is not None:
            usable_capacity = int(usable_capacity)
            
        timestamp_str = data.get("timestamp")
        facility_name = data.get("facility_name", "E-Library")

        result = predictor.predict(
            current_recorded_sessions=current_sessions,
            usable_capacity=usable_capacity,
            timestamp_str=timestamp_str,
            facility_name=facility_name
        )

        return jsonify(result), 200

    except Exception as e:
        return jsonify({
            "status": "ERROR",
            "message": f"Prediction failed: {str(e)}",
            "forecasts": [],
            "estimated_time_until_full_minutes": None,
            "time_until_full_message": "Error processing forecast request",
            "best_time_to_visit": "Unavailable"
        }), 500


@app.route("/metadata", methods=["GET"])
def get_metadata():
    if predictor.metadata:
        return jsonify(predictor.metadata), 200
    return jsonify({
        "status": "NOT_TRAINED",
        "message": "Model metadata unavailable. Please train models first."
    }), 404


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    print(f"\n=======================================================")
    print(f" QueueSense ML Inference Service Starting on Port {port}")
    print(f" Health check: http://localhost:{port}/health")
    print(f" Model ready: {predictor.is_ready()}")
    print(f"=======================================================\n")
    app.run(host="0.0.0.0", port=port, debug=False)

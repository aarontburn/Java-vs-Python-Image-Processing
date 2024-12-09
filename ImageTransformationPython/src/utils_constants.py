"""
This file holds common constants, such as dictionary keys.
"""

# Event and response keys
IMAGE_FILE_KEY: str = 'image_file'
ERROR_KEY: str = 'error'
BUCKET_KEY: str = "bucketname"
FILE_NAME_KEY: str = "filename"
COLD_START_KEY: str = "cold_start"
IMAGE_URL_KEY: str = "url"
IMAGE_URL_EXPIRES_IN_KEY: str = "url_expires_in_seconds"
NETWORK_LATENCY_KEY: str = "network_latency_ms"
FUNCTION_RUN_TIME_KEY: str = "function_runtime_s"
ESTIMATED_COST_KEY: str = "estimated_cost_usd"
LANGUAGE_KEY: str = "language"
MEMORY_USED_MB_KEY: str = "memory_used_mb"
PROCESSING_THROUGHPUT_KEY: str = "processing_throughput"
ONLY_METRICS_KEY: str = "return_only_metrics"

START_TIME_KEY: str = "start_time";
END_TIME_KEY: str = "end_time";
# Default URL expiration time, in seconds
IMAGE_URL_EXPIRATION_SECONDS = 3600

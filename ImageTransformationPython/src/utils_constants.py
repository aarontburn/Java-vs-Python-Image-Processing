"""
This file holds common constants, such as dictionary keys.
"""

# Metrics
START_TIME_KEY: str = "start_time";
END_TIME_KEY: str = "end_time";
PROCESSING_THROUGHPUT_KEY: str = "processing_throughput"
MEMORY_USED_MB_KEY: str = "memory_used_mb"
ESTIMATED_COST_KEY: str = "estimated_cost_usd"
NETWORK_LATENCY_KEY: str = "network_latency_ms"
FUNCTION_RUN_TIME_KEY: str = "function_runtime_ms"
LANGUAGE_KEY: str = "language"
COLD_START_KEY: str = "cold_start"

# Request body keys
BUCKET_KEY: str = "bucketname"
FILE_NAME_KEY: str = "filename"
ONLY_METRICS_KEY: str = "return_only_metrics"
GET_DOWNLOAD_KEY: str = "get_download"

# Response Body Keys
ERROR_KEY: str = 'error'
IMAGE_URL_KEY: str = "url"
IMAGE_URL_EXPIRES_IN_KEY: str = "url_expires_in_seconds"
SUCCESS_KEY: str = "success"

# Others
IMAGE_FILE_KEY: str = 'image_file'


# Default URL expiration time, in seconds
IMAGE_URL_EXPIRATION_SECONDS = 3600


ALLOWED_FILE_EXTENSIONS: tuple[str] = ["png", "jpg", "jpeg"]
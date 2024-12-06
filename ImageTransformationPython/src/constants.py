# Event and response keys
IMAGE_FILE_KEY: str = 'image_file'
ERROR_KEY: str = 'error'
BUCKET_KEY: str = "bucketname"
FILE_NAME_KEY: str = "filename"
COLD_START_KEY: str = "cold_start"
IMAGE_URL_KEY: str = "url"
IMAGE_URL_EXPIRES_IN_KEY: str = "url_expires_in_seconds"
IMAGE_ACCESS_LATENCY_KEY: str = "network_latency_s"
FUNCTION_RUN_TIME_KEY: str = "function_runtime_s"
ESTIMATED_COST_KEY: str = "estimated_cost_usd"

# Default URL expiration time, in seconds
IMAGE_URL_EXPIRATION_SECONDS = 3600
FROM bash

# Step 1: create a file at build-time
RUN echo "This file was generated inside Docker at build time." > /tmp/generated.txt

# Step 2: at container start, copy that file into the mounted host folder (/output)
CMD ["sh", "-c", "cat /tmp/generated.txt"]

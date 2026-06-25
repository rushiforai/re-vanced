FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y --no-install-recommends \
        git wget unzip \
    && rm -rf /var/lib/apt/lists/*

# ── Android SDK (needed by the extensions Gradle modules) ──
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

ARG CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && wget -q "${CMDLINE_TOOLS_URL}" -O /tmp/tools.zip \
    && unzip -q /tmp/tools.zip -d "${ANDROID_HOME}/cmdline-tools" \
    && mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm /tmp/tools.zip \
    && yes | sdkmanager --licenses >/dev/null 2>&1 \
    && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

WORKDIR /build

# ── ReVanced Patches source (used as build base for the slim .rvp) ──
ARG PATCHES_TAG=v6.1.0
RUN git clone --depth 1 --branch "${PATCHES_TAG}" \
        https://github.com/ReVanced/revanced-patches.git /build/revanced-patches \
    || git clone --depth 1 --branch "${PATCHES_TAG}" \
        https://gitlab.com/ReVanced/revanced-patches.git /build/revanced-patches
RUN echo "sdk.dir=${ANDROID_HOME}" > /build/revanced-patches/local.properties

# Pre-cache Gradle wrapper + dependencies (speeds up runtime builds)
RUN cd /build/revanced-patches \
    && ./gradlew --no-daemon dependencies 2>&1 || true

ENTRYPOINT ["/build/build-slim-rvp.sh"]

FROM openjdk:17-jdk-slim as builder

# Set environment variables
ENV GRADLE_HOME=/opt/gradle
ENV GRADLE_VERSION=8.5
ENV PATH=$PATH:$GRADLE_HOME/bin

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install Gradle
RUN curl -sSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip \
    && unzip gradle.zip -d /opt \
    && mv /opt/gradle-${GRADLE_VERSION} $GRADLE_HOME \
    && rm gradle.zip

WORKDIR /app

COPY . .

RUN gradle build --no-daemon

EXPOSE 8888

CMD ["gradle", "run", "--no-daemon"]

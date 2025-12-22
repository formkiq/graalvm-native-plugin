package com.formkiq.gradle.services;

import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import org.gradle.api.logging.Logger;

/** {@link BuildImageResultCallback} logger. */
public class LoggingBuildImageResultCallback extends BuildImageResultCallback {

  private long lastPercent = -1;
  private final Logger log;

  /**
   * Create a Docker build logger.
   *
   * @param logger logging function (e.g. this::log)
   */
  public LoggingBuildImageResultCallback(final Logger logger) {
    this.log = logger;
  }

  @Override
  public void onNext(final BuildResponseItem item) {

    // Dockerfile step output
    if (log != null) {
      String s = item.getStream();
      if (s != null) {
        log.info(s.trim());
      }
    }

    // Layer download progress
    // (NO direct ProgressDetail reference)
    ResponseItem.ProgressDetail pd = item.getProgressDetail();
    if (pd != null && pd.getTotal() != null && pd.getCurrent() != null) {

      long current = pd.getCurrent();
      long total = pd.getTotal();

      if (total > 0) {
        long percent = (current * 100) / total;
        if (percent != lastPercent) {
          if (log != null) {
            log.info("Progress: {}%", percent);
          }

          lastPercent = percent;
        }
      }
    }

    // Error output
    if (log != null) {
      ResponseItem.ErrorDetail errorDetail = item.getErrorDetail();
      if (errorDetail != null) {
        log.error("ERROR: {}", errorDetail.getMessage());
      }
    }

    super.onNext(item);
  }

  @Override
  public void onError(final Throwable throwable) {
    if (log != null) {
      log.error("Docker build failed: {}", throwable.getMessage());
    }
    super.onError(throwable);
  }

  @Override
  public void onComplete() {
    if (log != null) {
      log.info("Docker build completed");
    }
    super.onComplete();
  }
}

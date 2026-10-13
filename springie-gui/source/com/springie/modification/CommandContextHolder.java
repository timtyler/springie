// This program has been placed into the public domain by its author.

package com.springie.modification;

/**
 * Holds the active CommandContext (Tim, 2026-10-13, Phase 1 FrEnd decoupling).
 *
 * <p>FrEnd sets this during startup. Commands call
 * {@code CommandContextHolder.get().postCleanup()} instead of
 * {@code FrEnd.postCleanup()}.
 */
public final class CommandContextHolder {
  private static CommandContext context;

  private CommandContextHolder() {}

  public static void set(final CommandContext ctx) {
    context = ctx;
  }

  public static CommandContext get() {
    return context;
  }
}

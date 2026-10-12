// This program has been placed into the public domain by its author.

package com.springie.messages;

/**
 * A provider of demo entries, discovered at runtime via {@link java.util.ServiceLoader}.
 * The springie-demos module provides the implementation; springie-core only
 * depends on this interface, so there is no compile-time cycle.
 *
 * <p>Implementations register their demos by calling
 * {@link DemoRegistry#register(String, java.util.function.Supplier)}.
 */
public interface DemoProvider {
  /** Registers this provider's demos with {@link DemoRegistry}. */
  void registerDemos();
}

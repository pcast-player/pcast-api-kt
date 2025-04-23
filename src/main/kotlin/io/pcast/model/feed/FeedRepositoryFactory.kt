package io.pcast.model.feed

/**
 * Factory for creating FeedRepository instances.
 * Returns FakeFeedRepository for tests and RealFeedRepository otherwise.
 */
object FeedRepositoryFactory {
    private var isTestEnvironment = false
    private val realRepository by lazy { RealFeedRepository() }
    private val fakeRepository by lazy { FakeFeedRepository() }

    /**
     * Set the environment to test mode.
     * This should be called before tests that require a FakeFeedRepository.
     */
    fun enableTestMode() {
        isTestEnvironment = true
    }

    /**
     * Reset the environment to production mode.
     * This can be called after tests to ensure subsequent operations use the real repository.
     */
    fun disableTestMode() {
        isTestEnvironment = false
    }

    /**
     * Get the appropriate repository based on the current environment.
     * @return FakeFeedRepository if in test mode, RealFeedRepository otherwise.
     */
    fun getRepository(): FeedRepository {
        return if (isTestEnvironment) fakeRepository else realRepository
    }
}
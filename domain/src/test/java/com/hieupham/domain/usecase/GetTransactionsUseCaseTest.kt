package com.hieupham.domain.usecase

import com.hieupham.domain.data.*
import com.hieupham.domain.entity.CompositeTransactions
import com.hieupham.domain.interactor.usecase.GetTransactionsUseCase
import com.hieupham.domain.repository.TransactionRepository
import com.hieupham.domain.util.random
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.*
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import java.net.SocketTimeoutException

open class GetTransactionsUseCaseTest : UseCaseTest() {

    @Mock
    lateinit var transactionRepo: TransactionRepository

    @InjectMocks
    lateinit var usecase: GetTransactionsUseCase

    @Test
    fun testNextTransactions_CurrentBlockNumberIsZero_NothingIsReturn() {
        usecase.blockNumber(0)
        usecase.blockHeight(BLOCK_HEIGHT)
        val observer = TestObserver<CompositeTransactions>()

        usecase.next().dataStream().subscribe(observer)

        observer.assertNoValues()
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertTerminated()
    }

    @Test
    fun testNextTransaction_CurrentBlockNumberGreaterThanZero_DataIsReturn() {
        val blockHeight = BLOCK_HEIGHT + 1
        val blockNumber = BLOCK_NUMBER + 1
        val expectedResult = compositeTransactions1()
        usecase.blockNumber(blockNumber)
        usecase.blockHeight(blockHeight)
        val observer = TestObserver<CompositeTransactions>()
        whenever(transactionRepo.getTransactions(any())).thenReturn(Maybe.just(expectedResult))

        usecase.next().dataStream().subscribe(observer)

        observer.assertValue(expectedResult)
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertTerminated()
    }

    @Test
    fun testNextTransaction_CurrentBlockNumberGreaterThanZeroNErrorIsOccurred_ErrorIsThrow() {
        val blockHeight = BLOCK_HEIGHT + 1
        val blockNumber = BLOCK_NUMBER + 1
        val expectedResult = sqlException()
        usecase.blockNumber(blockNumber)
        usecase.blockHeight(blockHeight)
        val observer = TestObserver<CompositeTransactions>()
        whenever(transactionRepo.getTransactions(any())).thenReturn(Maybe.error(expectedResult))

        usecase.next().dataStream().subscribe(observer)

        observer.assertError(expectedResult)
        observer.assertNoValues()
        observer.assertNotComplete()
        observer.assertTerminated()
    }

    @Test
    fun testRefreshTransactions_GetBlockHeightFailed_ErrorIsThrow() {
        val expectedResult = timeoutException()
        val observer = TestObserver<CompositeTransactions>()
        whenever(transactionRepo.getBlockHeight()).thenReturn(Single.error(expectedResult))

        usecase.refresh().dataStream().subscribe(observer)

        observer.assertError(expectedResult)
        observer.assertNoValues()
        observer.assertNotComplete()
        observer.assertTerminated()
        assertNull(usecase.blockNumber())
        assertNull(usecase.blockHeight())
    }

    @Test
    fun testRefreshTransactions_GetBlockHeightNTransactionsSuccess_DataIsReturn() {
        val expectedResult = compositeTransactions1()
        val observer = TestObserver<CompositeTransactions>()
        whenever(transactionRepo.getBlockHeight()).thenReturn(Single.just(BLOCK_HEIGHT))
        whenever(transactionRepo.getTransactions(any())).thenReturn(Maybe.just(expectedResult))

        usecase.refresh().dataStream().subscribe(observer)

        observer.assertValue(expectedResult)
        observer.assertNoErrors()
        observer.assertComplete()
        observer.assertTerminated()
        assertEquals(usecase.blockHeight(), BLOCK_HEIGHT)
        assertEquals(usecase.blockNumber(), BLOCK_HEIGHT)
    }

    @Test
    fun testRefreshTransactions_GetBlockHeightSuccessNGetTransactionsFailed_ErrorIsThrow() {
        val expectedResult = timeoutException()
        val observer = TestObserver<CompositeTransactions>()
        whenever(transactionRepo.getBlockHeight()).thenReturn(Single.just(BLOCK_HEIGHT))
        whenever(transactionRepo.getTransactions(any())).thenReturn(Maybe.error(expectedResult))

        usecase.refresh().dataStream().subscribe(observer)

        observer.assertError(expectedResult)
        observer.assertError(SocketTimeoutException::class.java)
        observer.assertNotComplete()
        observer.assertTerminated()
        assertEquals(usecase.blockHeight(), BLOCK_HEIGHT)
        assertEquals(usecase.blockNumber(), BLOCK_HEIGHT)
    }

    @Test
    fun testFetchLatestTransactions_GetBlockHeightNTransactionsSuccess_DataIsReturnNCurrentBlockIsNotLatest() {
        val expectedResult = compositeTransactions1()
        val observer = TestObserver<CompositeTransactions>()
        val blockNumber = BLOCK_NUMBER - (1..10).random()
        usecase.blockNumber(blockNumber)
        whenever(transactionRepo.getBlockHeight()).thenReturn(Single.just(BLOCK_HEIGHT))
        whenever(transactionRepo.getTransactions(any())).thenReturn(Maybe.just(expectedResult))

        usecase.fetchLatest().dataStream().subscribe(observer)

        observer.assertValue(expectedResult)
        observer.assertNoErrors()
        observer.assertComplete()
        observer.assertTerminated()
        assertEquals(usecase.blockHeight(), BLOCK_HEIGHT)
        assertEquals(usecase.blockNumber(), blockNumber)
        assertNotEquals(usecase.blockNumber(), BLOCK_HEIGHT)
    }

}
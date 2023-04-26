package org.hrabosch.configuration;

import org.hrabosch.batching.CsvCryptoFieldMapper;
import org.hrabosch.model.CryptoPrice;
import org.hrabosch.batching.CryptoPriceItemProcessor;
import org.hrabosch.repository.CryptoPriceRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;


@Configuration
public class BatchConfiguration {

    @Value("file:${input.sourceDir}${input.pattern}")
    private Resource[] inputFiles;

    @Value("${input.linesToSkip:1}")
    private int linesToSkip;

    @Value("#{T(java.util.Arrays).asList('${input.headers}')}")
    private String[] inputHeaders;

    @Value("${input.chunksize:10}")
    private int importCsvChunkSize;

    @Autowired
    private CryptoPriceRepository cryptoPriceRepository;

    @Autowired
    private CsvCryptoFieldMapper csvCryptoFieldMapper;

    @Bean
    public FlatFileItemReader<CryptoPrice> cryptoPriceReader() {
        return new FlatFileItemReaderBuilder<CryptoPrice>().name("cryptoPriceReader")
                .linesToSkip(linesToSkip)
                .delimited()
                .names(inputHeaders)
                .fieldSetMapper(csvCryptoFieldMapper)
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<CryptoPrice> multiResourceItemReader() {
        MultiResourceItemReader<CryptoPrice> resourceItemReader = new MultiResourceItemReader<>();
        resourceItemReader.setResources(inputFiles);
        resourceItemReader.setDelegate(cryptoPriceReader());
        return resourceItemReader;
    }

    @Bean
    public CryptoPriceItemProcessor cryptoPriceItemProcessor() {
        return new CryptoPriceItemProcessor();
    }

    @Bean
    public RepositoryItemWriter<CryptoPrice> cryptoPriceWriter(List<CryptoPrice> processedItems) {
        return new RepositoryItemWriterBuilder<CryptoPrice>()
                .repository(cryptoPriceRepository)
                .methodName("save")
                .build();
    }

    @Bean(name = "csvImportJob")
    public Job readCsvFiles(JobRepository jobRepository, Step step) {
        return new JobBuilder("csvImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }

    @Bean
    public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager, ItemWriter<CryptoPrice> writer) {
        return new StepBuilder("step", jobRepository)
                .<CryptoPrice, CryptoPrice> chunk(importCsvChunkSize, transactionManager)
                .reader(multiResourceItemReader())
                .processor(cryptoPriceItemProcessor())
                .writer(writer)
                .build();
    }
}

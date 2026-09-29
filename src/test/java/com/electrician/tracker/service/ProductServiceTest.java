package com.electrician.tracker.service;

import com.electrician.tracker.repository.QuoteItemRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.dto.ProductCreationResult;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.ProductRepository;
import com.electrician.tracker.service.exception.DuplicateNameException;
import com.electrician.tracker.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private final Product existing = new Product("Şalter 3x40A", ProductUnit.PIECE);

    private ProductRepository productRepository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        when(productRepository.findAll()).thenReturn(List.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ProductService(productRepository, mock(MaterialItemRepository.class),
                mock(QuoteItemRepository.class), mock(AccessControl.class));
    }

    @Test
    void reusesExistingProductWhenNormalizedNameMatches() {
        ProductCreationResult result = service.createOrReuse("  SALTER   3X40a ", null, null, ProductUnit.METER);

        assertThat(result.existing()).isTrue();
        assertThat(result.product()).isSameAs(existing);
        verify(productRepository, never()).save(any());
    }

    @Test
    void createsNewProductWithTrimmedNameWhenNoMatch() {
        ProductCreationResult result = service.createOrReuse("  Kablo NYM 3x2,5 ", null, null, ProductUnit.METER);

        assertThat(result.existing()).isFalse();
        assertThat(result.product().getName()).isEqualTo("Kablo NYM 3x2,5");
        assertThat(result.product().getUnit()).isEqualTo(ProductUnit.METER);
    }

    @Test
    void createOrReuseRequiresNameAndUnit() {
        assertThatThrownBy(() -> service.createOrReuse(" ", null, null, ProductUnit.PIECE))
                .isInstanceOf(ValidationException.class)
                .hasMessage("error.product.name.required");
        assertThatThrownBy(() -> service.createOrReuse("Priz", null, null, null))
                .hasMessage("error.product.unit.required");
    }

    @Test
    void createRejectsNameThatOnlyDiffersInTurkishCharactersOrCase() {
        assertThatThrownBy(() -> service.create(new Product("salter 3x40a", ProductUnit.PIECE)))
                .isInstanceOf(DuplicateNameException.class);
    }
}

package com.order.order.service;

import com.inventory.inventory.dto.InventoryDTO;
import com.order.order.common.ErrorOrderResponse;
import com.order.order.common.OrderRenponse;
import com.order.order.common.SuccessOrderResponse;
import com.order.order.dto.OrderDTO;
import com.order.order.model.Orders;
import com.order.order.repo.OrderRepo;
import com.product.product.dto.ProductDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final WebClient productWebClient;
    private final WebClient inventoryWebClient;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ModelMapper modelMapper;

    public OrderService(WebClient productWebClient, WebClient inventoryWebClient, OrderRepo orderRepo, ModelMapper modelMapper) {
        this.productWebClient = productWebClient;
        this.inventoryWebClient = inventoryWebClient;
        this.orderRepo = orderRepo;
        this.modelMapper = modelMapper;
    }

    public List<OrderDTO> getAllOrders() {
        List<Orders>orderList = orderRepo.findAll();
        return modelMapper.map(orderList, new TypeToken<List<OrderDTO>>(){}.getType());
    }

    public OrderRenponse saveOrder(OrderDTO orderDTO) {
        Integer itemId = orderDTO.getItemId();

        try {
            InventoryDTO inventoryResponse = inventoryWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/item/{itemId}").build(itemId))
                    .retrieve()
                    .bodyToMono(InventoryDTO.class)
                    .block();
            assert inventoryResponse != null;
            System.out.println(inventoryResponse.getProductId());

            ProductDTO productResponse = productWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/product/{productId}").build(inventoryResponse.getProductId()))
                    .retrieve()
                    .bodyToMono(ProductDTO.class)
                    .block();
            assert productResponse != null;

            if(inventoryResponse.getQuantity() > 0) {
                if (productResponse.getForSale() == 1) {
                    orderRepo.save(modelMapper.map(orderDTO, Orders.class));
                    return new SuccessOrderResponse(orderDTO);
                }
                else {
                    return new ErrorOrderResponse("This Item is not for sale!");
                }
            }  else {
                return new ErrorOrderResponse("Item is not available!");
            }
        } catch (WebClientResponseException e) {
            if(e.getStatusCode().is5xxServerError()){
                return new ErrorOrderResponse("Item not found!");
            }
        }
        return null;
    }

    public OrderDTO updateOrder(OrderDTO OrderDTO) {
        orderRepo.save(modelMapper.map(OrderDTO, Orders.class));
        return OrderDTO;
    }

    public String deleteOrder(Integer orderId) {
        orderRepo.deleteById(orderId);
        return "Order deleted";
    }

    public OrderDTO getOrderById(Integer orderId) {
        Orders order = orderRepo.getOrderById(orderId);
        return modelMapper.map(order, OrderDTO.class);
    }
}

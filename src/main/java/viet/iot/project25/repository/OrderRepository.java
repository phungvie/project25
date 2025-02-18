package viet.iot.project25.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import viet.iot.project25.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}

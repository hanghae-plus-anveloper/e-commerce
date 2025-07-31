DELIMITER //

CREATE PROCEDURE insert_order_items()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE pid INT;
  DECLARE oid INT;
  DECLARE qty INT;
  DECLARE price INT;
  DECLARE discount INT;
  DECLARE odate DATETIME;

  WHILE i <= 10000 DO
    SET oid = i;

    SELECT ordered_at INTO odate FROM `order` WHERE id = oid;

    SET pid = FLOOR(RAND()*100) + 1;
    SET qty = FLOOR(RAND()*5) + 1;
    SET price = FLOOR(RAND()*10000 + 1000);
    SET discount = IF(RAND() < 0.3, FLOOR(price * 0.1), 0);
    INSERT INTO order_item (order_id, product_id, price, quantity, discount_amount, ordered_at)
    VALUES (oid, pid, price, qty, discount, odate);

    SET pid = FLOOR(RAND()*100) + 1;
    SET qty = FLOOR(RAND()*5) + 1;
    SET price = FLOOR(RAND()*10000 + 1000);
    SET discount = IF(RAND() < 0.3, FLOOR(price * 0.1), 0);
    INSERT INTO order_item (order_id, product_id, price, quantity, discount_amount, ordered_at)
    VALUES (oid, pid, price, qty, discount, odate);

    SET i = i + 1;
  END WHILE;
END //

DELIMITER ;

CALL insert_order_items();
DROP PROCEDURE insert_order_items;

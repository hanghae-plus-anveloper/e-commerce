DELIMITER //

CREATE PROCEDURE insert_products()
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= 100 DO
    INSERT INTO product (name, price, stock, version)
    VALUES (CONCAT('Product_', LPAD(i, 3, '0')), FLOOR(RAND()*10000 + 1000), 9999, 0);
    SET i = i + 1;
  END WHILE;
END //

DELIMITER ;

CALL insert_products();
DROP PROCEDURE insert_products;

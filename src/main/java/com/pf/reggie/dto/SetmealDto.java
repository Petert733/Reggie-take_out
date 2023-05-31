package com.pf.reggie.dto;

import com.pf.reggie.entity.Setmeal;
import com.pf.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}

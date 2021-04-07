package com.lesssoda.miaosha.dao;

import com.lesssoda.miaosha.dataobject.SequenceDO;
import org.springframework.stereotype.Repository;


@Repository
public interface SequenceDOMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Fri Mar 26 15:49:40 GMT+08:00 2021
     */
    int deleteByPrimaryKey(String name);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Fri Mar 26 15:49:40 GMT+08:00 2021
     */
    int insert(SequenceDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Fri Mar 26 15:49:40 GMT+08:00 2021
     */
    int insertSelective(SequenceDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Fri Mar 26 15:49:40 GMT+08:00 2021
     */
    SequenceDO selectByPrimaryKey(String name);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Fri Mar 26 15:49:40 GMT+08:00 2021
     */
    int updateByPrimaryKeySelective(SequenceDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table sequence_info
     *
     * @mbg.generated Fri Mar 26 15:49:40 GMT+08:00 2021
     */
    int updateByPrimaryKey(SequenceDO record);

    /**
     * 根据name查找sequence 并且上锁
     * @param name
     * @return
     */
    SequenceDO selectByName(String name);
}